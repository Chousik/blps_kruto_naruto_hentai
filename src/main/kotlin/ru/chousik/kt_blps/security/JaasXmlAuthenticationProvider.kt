package ru.chousik.blps_kt.security

import jakarta.annotation.PostConstruct
import javax.security.auth.login.AppConfigurationEntry
import javax.security.auth.login.Configuration
import javax.security.auth.login.LoginContext
import javax.security.auth.login.LoginException
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Component

@Component
class JaasXmlAuthenticationProvider(
    private val xmlAccountRegistry: XmlAccountRegistry,
    private val rolePrivilegeModel: RolePrivilegeModel,
    @Value("\${blps.security.accounts-location:\${user.dir}/data/security/accounts.xml}")
    private val accountsLocation: String,
    @Value("\${blps.security.accounts-bootstrap-location:classpath:security/accounts.xml}")
    private val bootstrapLocation: String
) : AuthenticationProvider {

    private val jaasConfiguration = object : Configuration() {
        override fun getAppConfigurationEntry(name: String?): Array<AppConfigurationEntry> =
            arrayOf(
                AppConfigurationEntry(
                    XmlAccountsLoginModule::class.java.name,
                    AppConfigurationEntry.LoginModuleControlFlag.REQUIRED,
                    mapOf(
                        "accountsLocation" to accountsLocation,
                        "bootstrapLocation" to bootstrapLocation
                    )
                )
            )
    }

    @PostConstruct
    fun validateAccounts() {
        XmlAccountsSupport.loadAccounts(accountsLocation, bootstrapLocation)
    }

    override fun authenticate(authentication: Authentication): Authentication {
        val username = authentication.name?.trim().orEmpty()
        val password = authentication.credentials?.toString().orEmpty()

        try {
            LoginContext(
                "BLPS_XML_ACCOUNTS",
                null,
                JaasUsernamePasswordCallbackHandler(username, password),
                jaasConfiguration
            ).login()
        } catch (ex: LoginException) {
            throw BadCredentialsException("bad credentials", ex)
        }

        val account = xmlAccountRegistry.getByUsername(username)
        val authorities = rolePrivilegeModel.authoritiesFor(account.role)
        val principal = AuthenticatedAccount(
            username = account.username,
            userId = account.userId,
            role = account.role,
            authorities = authorities.mapNotNull { it.authority }.toSet()
        )

        return UsernamePasswordAuthenticationToken.authenticated(principal, null, authorities)
    }

    override fun supports(authentication: Class<*>): Boolean =
        UsernamePasswordAuthenticationToken::class.java.isAssignableFrom(authentication)
}
