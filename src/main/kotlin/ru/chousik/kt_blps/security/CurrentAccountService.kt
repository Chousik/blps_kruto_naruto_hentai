package ru.chousik.kt_blps.security

import java.security.Principal
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException
import org.springframework.security.authentication.InsufficientAuthenticationException
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service

@Service
class CurrentAccountService {

    fun currentAccount(): AuthenticatedAccount =
        fromAuthentication(SecurityContextHolder.getContext().authentication)

    fun fromPrincipal(principal: Principal?): AuthenticatedAccount =
        when (principal) {
            is Authentication -> fromAuthentication(principal)
            is AuthenticatedAccount -> principal
            null -> throw AuthenticationCredentialsNotFoundException("authentication is required")
            else -> throw InsufficientAuthenticationException("unsupported authenticated principal")
        }

    fun fromAuthentication(authentication: Authentication?): AuthenticatedAccount {
        if (authentication == null || !authentication.isAuthenticated) {
            throw AuthenticationCredentialsNotFoundException("authentication is required")
        }

        val principal = authentication.principal
        return when (principal) {
            is AuthenticatedAccount -> principal
            else -> throw InsufficientAuthenticationException("unsupported authenticated principal")
        }
    }
}
