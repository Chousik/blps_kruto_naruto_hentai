package ru.chousik.blps_kt.security

import java.io.FileInputStream
import java.io.InputStream
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.UUID
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import org.w3c.dom.Element
import kotlin.concurrent.withLock
import java.util.concurrent.locks.ReentrantLock
import ru.chousik.blps_kt.model.UserRole

object XmlAccountsSupport {
    private val writeLock = ReentrantLock()

    fun loadAccounts(location: String, bootstrapLocation: String? = null): List<XmlAccountDefinition> {
        ensureBootstrapped(location, bootstrapLocation)
        return openStream(location).use { input ->
            val document = parseDocument(input)
            val nodes = document.getElementsByTagName("account")
            buildList(nodes.length) {
                for (index in 0 until nodes.length) {
                    val element = nodes.item(index) as Element
                    add(
                        XmlAccountDefinition(
                            username = element.getAttribute("username"),
                            passwordHash = element.getAttribute("password-hash").ifBlank {
                                element.getAttribute("password")
                            },
                            role = UserRole.valueOf(element.getAttribute("role")),
                            userId = UUID.fromString(element.getAttribute("user-id"))
                        )
                    )
                }
            }
        }
    }

    fun appendAccount(location: String, account: XmlAccountDefinition, bootstrapLocation: String? = null) {
        withWriteLock {
            ensureBootstrapped(location, bootstrapLocation)
            val path = resolveWritablePath(location)
            val document = if (Files.exists(path)) {
                Files.newInputStream(path).use(::parseDocument)
            } else {
                val emptyDocument = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument()
                emptyDocument.appendChild(emptyDocument.createElement("accounts"))
                emptyDocument
            }

            val root = document.documentElement ?: document.appendChild(document.createElement("accounts"))
            val element = document.createElement("account")
            element.setAttribute("username", account.username)
            element.setAttribute("password-hash", account.passwordHash)
            element.setAttribute("role", account.role.name)
            element.setAttribute("user-id", account.userId.toString())
            root.appendChild(element)

            writeDocument(path, document)
        }
    }

    fun <T> withWriteLock(action: () -> T): T =
        writeLock.withLock(action)

    private fun parseDocument(input: InputStream) =
        DocumentBuilderFactory.newInstance()
            .newDocumentBuilder()
            .parse(input)

    private fun ensureBootstrapped(location: String, bootstrapLocation: String?) {
        if (location.startsWith("classpath:")) {
            return
        }

        val path = resolveWritablePath(location)
        if (Files.exists(path)) {
            return
        }

        val sourceLocation = bootstrapLocation?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("accounts file '$location' does not exist and no bootstrap location is configured")

        withWriteLock {
            if (Files.exists(path)) {
                return@withWriteLock
            }

            val targetDir = path.parent ?: throw IllegalStateException("accounts file must have a parent directory")
            Files.createDirectories(targetDir)
            openStream(sourceLocation).use { input ->
                Files.copy(input, path, StandardCopyOption.REPLACE_EXISTING)
            }
        }
    }

    private fun writeDocument(path: Path, document: org.w3c.dom.Document) {
        val targetDir = path.parent ?: throw IllegalStateException("accounts file must have a parent directory")
        Files.createDirectories(targetDir)

        val tempFile = Files.createTempFile(targetDir, "accounts-", ".xml")
        Files.newOutputStream(tempFile).use { output ->
            val transformer = TransformerFactory.newInstance().newTransformer().apply {
                setOutputProperty(OutputKeys.ENCODING, "UTF-8")
                setOutputProperty(OutputKeys.INDENT, "yes")
                setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4")
            }
            transformer.transform(DOMSource(document), StreamResult(output))
        }

        try {
            Files.move(
                tempFile,
                path,
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE
            )
        } catch (_: Exception) {
            Files.move(tempFile, path, StandardCopyOption.REPLACE_EXISTING)
        }
    }

    private fun openStream(location: String): InputStream =
        if (location.startsWith("classpath:")) {
            val resourcePath = location.removePrefix("classpath:")
            Thread.currentThread().contextClassLoader.getResourceAsStream(resourcePath)
                ?: throw IllegalStateException("cannot load accounts resource '$location'")
        } else if (location.startsWith("file:")) {
            Files.newInputStream(Path.of(URI.create(location)))
        } else {
            FileInputStream(location)
        }

    private fun resolveWritablePath(location: String): Path =
        when {
            location.startsWith("classpath:") -> {
                val resourcePath = location.removePrefix("classpath:")
                val resource = Thread.currentThread().contextClassLoader.getResource(resourcePath)
                    ?: throw IllegalStateException("cannot resolve writable accounts resource '$location'")
                if (resource.protocol != "file") {
                    throw IllegalStateException("accounts resource '$location' is not writable")
                }
                Path.of(resource.toURI())
            }

            location.startsWith("file:") -> Path.of(URI.create(location))
            else -> Path.of(location)
        }
}
