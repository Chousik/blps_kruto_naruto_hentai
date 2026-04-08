package ru.chousik.kt_blps.security

import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class XmlAccountRegistry(
    @Value("\${blps.security.accounts-location:\${user.dir}/data/security/accounts.xml}")
    private val accountsLocation: String
) {
    @PostConstruct
    fun validateAccounts() {
        XmlAccountsSupport.loadAccounts(accountsLocation)
    }

    fun findAll(): List<XmlAccountDefinition> =
        XmlAccountsSupport.loadAccounts(accountsLocation)

    fun findByUsername(username: String): XmlAccountDefinition? =
        findAll().firstOrNull { it.username == username }

    fun appendAccount(account: XmlAccountDefinition) {
        XmlAccountsSupport.appendAccount(accountsLocation, account)
    }
}
