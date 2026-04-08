package ru.chousik.kt_blps.security

import java.util.UUID
import ru.chousik.kt_blps.model.UserRole

data class XmlAccountDefinition(
    val username: String,
    val passwordHash: String,
    val userId: UUID,
    val role: UserRole
)
