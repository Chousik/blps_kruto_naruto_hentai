package ru.chousik.kt_blps.api.security

import java.util.UUID
import ru.chousik.kt_blps.model.UserRole
import ru.chousik.kt_blps.security.AuthenticatedAccount

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

