package ru.chousik.blps_kt.security

import java.security.Principal
import java.util.UUID
import ru.chousik.blps_kt.model.UserRole

data class AuthenticatedAccount(
    val username: String,
    val userId: UUID,
    val role: UserRole,
    val authorities: Set<String>
) : Principal {
    override fun getName(): String = username

    fun hasAuthority(authority: String): Boolean = authority in authorities
}

