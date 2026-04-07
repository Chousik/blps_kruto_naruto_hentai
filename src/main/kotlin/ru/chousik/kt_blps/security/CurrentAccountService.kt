package ru.chousik.blps_kt.security

import java.security.Principal
import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException

@Service
class CurrentAccountService(
    private val xmlAccountRegistry: XmlAccountRegistry,
    private val rolePrivilegeModel: RolePrivilegeModel
) {

    fun currentAccount(): AuthenticatedAccount =
        fromAuthentication(SecurityContextHolder.getContext().authentication)

    fun fromPrincipal(principal: Principal?): AuthenticatedAccount =
        when (principal) {
            is Authentication -> fromAuthentication(principal)
            is AuthenticatedAccount -> principal
            null -> throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "authenticated principal is required")
            else -> fromUsername(principal.name)
        }

    fun fromAuthentication(authentication: Authentication?): AuthenticatedAccount {
        if (authentication == null || !authentication.isAuthenticated) {
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "authentication is required")
        }

        val principal = authentication.principal
        return when (principal) {
            is AuthenticatedAccount -> principal
            else -> throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "unsupported authenticated principal")
        }
    }

    private fun fromUsername(username: String?): AuthenticatedAccount {
        val normalizedUsername = username?.trim()
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "authenticated principal is required")
        if (normalizedUsername.isBlank()) {
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "authenticated principal is required")
        }

        val account = xmlAccountRegistry.getByUsername(normalizedUsername)
        val authorities = rolePrivilegeModel.authoritiesFor(account.role)
            .mapNotNull { it.authority }
            .toSet()

        return AuthenticatedAccount(
            username = account.username,
            userId = account.userId,
            role = account.role,
            authorities = authorities
        )
    }
}
