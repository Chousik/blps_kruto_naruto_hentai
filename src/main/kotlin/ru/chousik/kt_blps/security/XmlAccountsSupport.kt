package ru.chousik.kt_blps.security

import jakarta.xml.bind.JAXBContext
import jakarta.xml.bind.Marshaller
import java.io.FileInputStream
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

object XmlAccountsSupport {
    private val writeLock = ReentrantLock()
    private val xmlContext: JAXBContext = JAXBContext.newInstance(XmlAccountsDocument::class.java)

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
        xmlContext.createUnmarshaller().unmarshal(input) as XmlAccountsDocument

    private fun writeDocument(path: Path, document: XmlAccountsDocument) {
        val parent = path.parent ?: Path.of(".")
        Files.createDirectories(parent)
        val tempFile = Files.createTempFile(parent, "accounts-", ".xml")
        Files.newOutputStream(tempFile).use { output ->
            xmlContext.createMarshaller().apply {
                setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true)
                setProperty(Marshaller.JAXB_ENCODING, "UTF-8")
            }.marshal(document, output)
        }
        Files.move(tempFile, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
    }

    private fun openStream(location: String): InputStream =
        FileInputStream(location)
}
