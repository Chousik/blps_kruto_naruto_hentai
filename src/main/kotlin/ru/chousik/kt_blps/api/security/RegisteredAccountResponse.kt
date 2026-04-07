package ru.chousik.kt_blps.api.security

import java.util.UUID
import ru.chousik.kt_blps.model.UserRole

data class RegisteredAccountResponse(
    val userId: UUID,
    val username: String,
    val email: String,
    val role: UserRole
)
