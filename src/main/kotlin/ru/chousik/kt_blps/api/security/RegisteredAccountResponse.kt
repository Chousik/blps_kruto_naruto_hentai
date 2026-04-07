package ru.chousik.blps_kt.api.security

import java.util.UUID
import ru.chousik.blps_kt.model.UserRole

data class RegisteredAccountResponse(
    val userId: UUID,
    val username: String,
    val email: String,
    val role: UserRole
)
