package ru.chousik.kt_blps.security

import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException

@Service
class XmlAccountRegistry(
    @Value("\${blps.security.accounts-location:\${user.dir}/data/security/accounts.xml}")
    private val accountsLocation: String
) {

    fun findAll(): List<XmlAccountDefinition> =
        XmlAccountsSupport.loadAccounts(accountsLocation)

    fun findByUsername(username: String): XmlAccountDefinition? =
        findAll().firstOrNull { it.username == username }

    fun getByUsername(username: String): XmlAccountDefinition =
        findByUsername(username)
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "authenticated account not found in XML registry")

    fun appendAccount(account: XmlAccountDefinition) {
        XmlAccountsSupport.appendAccount(accountsLocation, account)
    }
}
