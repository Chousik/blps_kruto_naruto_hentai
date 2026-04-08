package ru.chousik.kt_blps.security

import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import javax.security.auth.login.AppConfigurationEntry
import javax.security.auth.login.Configuration
import javax.security.auth.login.LoginContext
import javax.security.auth.login.LoginException

@Component
    class CustomAuthenticationProvider(
    private val passwordEncoder: PasswordEncoder,
    private val xmlAccountRegistry: XmlAccountRegistry
) : AuthenticationProvider {

    private val jaasConfiguration = object : Configuration() {
        override fun getAppConfigurationEntry(name: String?): Array<AppConfigurationEntry> =
            arrayOf(
                AppConfigurationEntry(
                    CustomLoginModule::class.java.name,
                    AppConfigurationEntry.LoginModuleControlFlag.REQUIRED,
                    mapOf(
                        "passwordEncoder" to passwordEncoder,
                        "xmlAccountRegistry" to xmlAccountRegistry
                    )
                )
            )
    }

    override fun authenticate(authentication: Authentication): Authentication? {
        val username = authentication.name?.trim().orEmpty()
        val password = authentication.credentials?.toString().orEmpty()
        val loginContext = LoginContext(
            "BLPS_XML_ACCOUNTS",
            null,
            CustomUsernamePasswordCallbackHandler(username, password),
            jaasConfiguration
        )
        try {
            loginContext.login()

        } catch (ex: LoginException) {
            throw BadCredentialsException("bad credentials", ex)
        }
        val subject = loginContext.subject

        val principal = subject.principals
            .filterIsInstance<XmlAccountPrincipal>()
            .firstOrNull()
            ?: throw BadCredentialsException("bad credentials")
        val authorities = RolePrivilegeModel.authoritiesFor(principal.role)
        return UsernamePasswordAuthenticationToken.authenticated(principal, null, authorities)
    }

    override fun supports(authentication: Class<*>): Boolean {
        return UsernamePasswordAuthenticationToken::class.java.isAssignableFrom(authentication)
    }
}