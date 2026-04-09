package ru.chousik.kt_blps.security

import org.springframework.security.crypto.password.PasswordEncoder
import javax.security.auth.Subject
import javax.security.auth.callback.Callback
import javax.security.auth.callback.CallbackHandler
import javax.security.auth.callback.NameCallback
import javax.security.auth.callback.PasswordCallback
import javax.security.auth.login.FailedLoginException
import javax.security.auth.spi.LoginModule

class CustomLoginModule : LoginModule {
    private lateinit var xmlAccountRegistry: XmlAccountRegistry
    private lateinit var passwordEncoder: PasswordEncoder
    private lateinit var subject: Subject
    private lateinit var callbackHandler: CallbackHandler
    private lateinit var options: Map<String, *>
    private var authenticatedAccount: XmlAccountDefinition? = null

    override fun initialize(
        subject: Subject,
        callbackHandler: CallbackHandler,
        sharedState: Map<String?, *>,
        options: Map<String, *>?
    ) {
        this.subject = subject
        this.callbackHandler = callbackHandler
        this.options = options.orEmpty()
        this.passwordEncoder = this.options["passwordEncoder"] as PasswordEncoder
        this.xmlAccountRegistry = this.options["xmlAccountRegistry"] as XmlAccountRegistry
    }

    override fun login(): Boolean {
        val nameCallback = NameCallback("username")
        val passwordCallback = PasswordCallback("password", false)
        callbackHandler.handle(arrayOf<Callback>(nameCallback, passwordCallback))

        val username = nameCallback.name?.trim().orEmpty()
        val password = String(passwordCallback.password)
        passwordCallback.clearPassword()

        val account = xmlAccountRegistry.findByUsername(username)
            ?: throw FailedLoginException("user not found")
        if (!passwordEncoder.matches(password, account.passwordHash)) {
            throw FailedLoginException("password mismatch")
        }
        authenticatedAccount = account
        return true
    }

    override fun commit(): Boolean {
        authenticatedAccount?.let { account ->
            val principals = subject.principals
            principals.add(account.toPrincipal())
            return true
        }
        return false
    }

    override fun abort(): Boolean {
        authenticatedAccount = null
        return true
    }

    override fun logout(): Boolean {
        return true
    }
}