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
            readDocument(input).accounts.map { it.toDefinition() }
        }
    }

    fun appendAccount(location: String, account: XmlAccountDefinition) {
        writeLock.withLock {
            ensureFileExists(location)
            val path = Path.of(location)
            val document = if (Files.exists(path)) readDocument(Files.newInputStream(path)) else XmlAccountsDocument()

            document.accounts += XmlStoredAccount.from(account)
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
            allowTypes(arrayOf(XmlAccountsDocument::class.java, XmlStoredAccount::class.java, UserRole::class.java))
            alias("accounts", XmlAccountsDocument::class.java)
            alias("account", XmlStoredAccount::class.java)
            addImplicitCollection(XmlAccountsDocument::class.java, "accounts", "account", XmlStoredAccount::class.java)
            useAttributeFor(XmlStoredAccount::class.java, "username")
            useAttributeFor(XmlStoredAccount::class.java, "passwordHash")
            useAttributeFor(XmlStoredAccount::class.java, "role")
            useAttributeFor(XmlStoredAccount::class.java, "userId")
            aliasField("password-hash", XmlStoredAccount::class.java, "passwordHash")
            aliasField("user-id", XmlStoredAccount::class.java, "userId")
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

    data class XmlAccountsDocument(val accounts: MutableList<XmlStoredAccount> = mutableListOf())

    data class XmlStoredAccount(
        var username: String = "",
        var passwordHash: String = "",
        var role: UserRole = UserRole.GUEST,
        var userId: String = ""
    ) {
        fun toDefinition(): XmlAccountDefinition =
            XmlAccountDefinition(username, passwordHash, role, java.util.UUID.fromString(userId))

        companion object {
            fun from(account: XmlAccountDefinition): XmlStoredAccount =
                XmlStoredAccount(account.username, account.passwordHash, account.role, account.userId.toString())
        }
    }
}