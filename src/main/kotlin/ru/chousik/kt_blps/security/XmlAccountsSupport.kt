package ru.chousik.kt_blps.security

import com.thoughtworks.xstream.XStream
import com.thoughtworks.xstream.io.xml.StaxDriver
import java.io.FileInputStream
import java.io.InputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import ru.chousik.kt_blps.model.UserRole
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

object XmlAccountsSupport {
    private val writeLock = ReentrantLock()

    fun loadAccounts(location: String): List<XmlAccountDefinition> {
        ensureFileExists(location)
        return openStream(location).use { input ->
            readDocument(input).accounts.toList()
        }
    }

    fun appendAccount(location: String, account: XmlAccountDefinition) {
        writeLock.withLock {
            ensureFileExists(location)
            val path = Path.of(location)
            val document = if (Files.exists(path)) readDocument(Files.newInputStream(path)) else XmlAccountsDocument()

            document.accounts += account
            writeDocument(path, document)
        }
    }

    private fun ensureFileExists(location: String) {
        val path = Path.of(location)
        if (Files.exists(path)) return

        val document = XmlAccountsDocument()
        writeDocument(path, document)
    }

    private fun readDocument(input: InputStream): XmlAccountsDocument =
        XStream(StaxDriver()).apply {
            allowTypes(arrayOf(XmlAccountsDocument::class.java, XmlAccountDefinition::class.java, UserRole::class.java))
            alias("accounts", XmlAccountsDocument::class.java)
            alias("account", XmlAccountDefinition::class.java)
            addImplicitCollection(XmlAccountsDocument::class.java, "accounts", "account", XmlAccountDefinition::class.java)
            useAttributeFor(XmlAccountDefinition::class.java, "username")
            useAttributeFor(XmlAccountDefinition::class.java, "userId")
            useAttributeFor(XmlAccountDefinition::class.java, "passwordHash")
            useAttributeFor(XmlAccountDefinition::class.java, "role")
            aliasField("password-hash", XmlAccountDefinition::class.java, "passwordHash")
            aliasField("user-id", XmlAccountDefinition::class.java, "userId")
        }.fromXML(input) as XmlAccountsDocument

    private fun writeDocument(path: Path, document: XmlAccountsDocument) {
        val tempFile = Files.createTempFile(path.parent, "accounts-", ".xml")
        Files.newOutputStream(tempFile).use { output ->
            OutputStreamWriter(output, StandardCharsets.UTF_8).use { writer ->
                writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
                XStream(StaxDriver()).toXML(document, writer)
            }
        }
        Files.move(tempFile, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
    }

    private fun openStream(location: String): InputStream =
        FileInputStream(location)

    data class XmlAccountsDocument(val accounts: MutableList<XmlAccountDefinition> = mutableListOf())
}