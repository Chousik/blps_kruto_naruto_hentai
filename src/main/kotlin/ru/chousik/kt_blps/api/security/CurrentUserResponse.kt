package ru.chousik.blps_kt.api.security

import java.util.UUID
import ru.chousik.blps_kt.model.UserRole
import ru.chousik.blps_kt.security.AuthenticatedAccount

data class CurrentUserResponse(
    val username: String,
    val userId: UUID,
    val role: UserRole,
    val authorities: List<String>
) {
    companion object {
        fun from(account: AuthenticatedAccount): CurrentUserResponse =
            CurrentUserResponse(
                username = account.username,
                userId = account.userId,
                role = account.role,
                authorities = account.authorities.sorted()
            )
    }
}

