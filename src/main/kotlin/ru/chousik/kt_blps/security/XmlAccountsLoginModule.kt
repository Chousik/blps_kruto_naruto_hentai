package ru.chousik.blps_kt.security

import java.security.Principal
import javax.security.auth.Subject
import javax.security.auth.callback.Callback
import javax.security.auth.callback.CallbackHandler
import javax.security.auth.callback.NameCallback
import javax.security.auth.callback.PasswordCallback
import javax.security.auth.login.FailedLoginException
import javax.security.auth.spi.LoginModule
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder

class XmlAccountsLoginModule : LoginModule {
    private val passwordEncoder = BCryptPasswordEncoder()

    private lateinit var subject: Subject
    private lateinit var callbackHandler: CallbackHandler
    private lateinit var options: Map<String, *>
    private var authenticatedAccount: XmlAccountDefinition? = null
    private val principals: MutableSet<Principal> = linkedSetOf()

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
        val password = String(passwordCallback.password ?: CharArray(0))
        passwordCallback.clearPassword()

        val accountsLocation = options["accountsLocation"]?.toString()
            ?: throw FailedLoginException("accountsLocation JAAS option is required")
        val bootstrapLocation = options["bootstrapLocation"]?.toString()
        val account = XmlAccountsSupport.loadAccounts(accountsLocation, bootstrapLocation)
            .firstOrNull { it.username == username }
            ?: throw FailedLoginException("bad credentials")

        if (!passwordEncoder.matches(password, account.passwordHash)) {
            throw FailedLoginException("bad credentials")
        }

        authenticatedAccount = account
        return true
    }

    override fun commit(): Boolean {
        val account = authenticatedAccount ?: return false
        principals += Principal { account.username }
        principals += Principal { account.role.name }
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
}
