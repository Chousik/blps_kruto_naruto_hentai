package ru.chousik.kt_blps.security

import java.security.Principal
import javax.security.auth.login.AppConfigurationEntry
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.authentication.AuthenticationServiceException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.authentication.jaas.AuthorityGranter
import org.springframework.security.authentication.jaas.DefaultJaasAuthenticationProvider
import org.springframework.security.authentication.jaas.memory.InMemoryConfiguration
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Component

@Component
class JaasXmlAuthenticationProvider(
    private val xmlAccountRegistry: XmlAccountRegistry,
    rolePrivilegeModel: RolePrivilegeModel,
    @Value("\${blps.security.accounts-location:\${user.dir}/data/security/accounts.xml}")
    accountsLocation: String
) : DefaultJaasAuthenticationProvider() {

    init {
        setLoginContextName(LOGIN_CONTEXT_NAME)
        setConfiguration(
            InMemoryConfiguration(
                mapOf(
                    LOGIN_CONTEXT_NAME to arrayOf(
                        AppConfigurationEntry(
                            XmlAccountsLoginModule::class.java.name,
                            AppConfigurationEntry.LoginModuleControlFlag.REQUIRED,
                            mapOf("accountsLocation" to accountsLocation)
                        )
                    )
                )
            )
        )
        setAuthorityGranters(arrayOf(XmlAccountAuthorityGranter(rolePrivilegeModel)))
    }

    override fun authenticate(auth: Authentication): Authentication? {
        val result = super.authenticate(auth) ?: return null
        val account = xmlAccountRegistry.findByUsername(result.name)
            ?: throw AuthenticationServiceException("JAAS authenticated account is missing from XML registry")

        return UsernamePasswordAuthenticationToken.authenticated(
            AuthenticatedAccount(
                username = account.username,
                userId = account.userId,
                role = account.role,
                authorities = result.authorities.mapTo(linkedSetOf()) { it.authority }
            ),
            null,
            result.authorities
        ).apply {
            details = result.details
        }
    }

    private class XmlAccountAuthorityGranter(
        private val rolePrivilegeModel: RolePrivilegeModel
    ) : AuthorityGranter {

        override fun grant(principal: Principal): Set<String> =
            when (principal) {
                is XmlAccountPrincipal -> rolePrivilegeModel.authoritiesFor(principal.role)
                    .mapTo(linkedSetOf()) { it.authority }
                else -> emptySet()
            }
    }

    companion object {
        private const val LOGIN_CONTEXT_NAME = "BLPS_XML_ACCOUNTS"
    }
}
