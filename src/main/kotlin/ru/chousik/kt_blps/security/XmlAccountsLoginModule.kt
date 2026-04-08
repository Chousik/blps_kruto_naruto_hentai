package ru.chousik.kt_blps.security

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import javax.security.auth.Subject
import javax.security.auth.callback.Callback
import javax.security.auth.callback.CallbackHandler
import javax.security.auth.callback.NameCallback
import javax.security.auth.callback.PasswordCallback
import javax.security.auth.login.FailedLoginException
import javax.security.auth.spi.LoginModule

class XmlAccountsLoginModule : LoginModule {
    private val passwordEncoder = BCryptPasswordEncoder()

    private lateinit var subject: Subject
    private lateinit var callbackHandler: CallbackHandler
    private lateinit var options: Map<String, *>
    private var authenticatedAccount: XmlAccountDefinition? = null
    private val principals = mutableSetOf<XmlAccountPrincipal>()

    override fun initialize(
        subject: Subject,
        callbackHandler: CallbackHandler,
        sharedState: MutableMap<String, *>?,
        options: MutableMap<String, *>?
    ) {
        this.subject = subject
        this.callbackHandler = callbackHandler
        this.options = options.orEmpty()
    }

    override fun login(): Boolean {
        val nameCallback = NameCallback("username")
        val passwordCallback = PasswordCallback("password", false)
        callbackHandler.handle(arrayOf<Callback>(nameCallback, passwordCallback))

        val username = nameCallback.name?.trim().orEmpty()
        val password = passwordCallback.password ?: CharArray(0)
        val accountsLocation = getOption("accountsLocation")

        return try {
            val account = XmlAccountsSupport.loadAccounts(accountsLocation)
                .firstOrNull { it.username == username }
                ?: throw FailedLoginException("bad credentials")

            if (!passwordEncoder.matches(String(password), account.passwordHash)) {
                throw FailedLoginException("bad credentials")
            }

            authenticatedAccount = account
            true
        } finally {
            password.fill('\u0000')
            passwordCallback.clearPassword()
        }
    }

    override fun commit(): Boolean {
        val account = authenticatedAccount ?: return false
        principals += XmlAccountPrincipal(account.username, account.userId, account.role)
        subject.principals.addAll(principals)
        return true
    }

    override fun abort(): Boolean {
        logout()
        return true
    }

    override fun logout(): Boolean {
        subject.principals.removeAll(principals)
        principals.clear()
        authenticatedAccount = null
        return true
    }

    private fun getOption(key: String): String =
        options[key]?.toString() ?: throw FailedLoginException("$key JAAS option is required")
}
