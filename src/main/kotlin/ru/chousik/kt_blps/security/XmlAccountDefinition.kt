package ru.chousik.blps_kt.security

import java.util.UUID
import ru.chousik.blps_kt.model.UserRole

data class XmlAccountDefinition(
    val username: String,
    val passwordHash: String,
    val role: UserRole,
    val userId: UUID
)
