package ru.chousik.kt_blps.dto.auth

import java.time.OffsetDateTime
import java.util.UUID
import ru.chousik.kt_blps.model.User
import ru.chousik.kt_blps.model.UserRole

data class RegisterResponse(
    val id: UUID,
    val username: String,
    val email: String,
    val firstName: String,
    val lastName: String,
    val role: UserRole,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime
) {
    companion object {
        fun from(user: User): RegisterResponse = RegisterResponse(
            id = user.id,
            username = user.username,
            email = user.email,
            firstName = user.firstName,
            lastName = user.lastName,
            role = user.role,
            createdAt = user.createdAt,
            updatedAt = user.updatedAt
        )
    }
}
