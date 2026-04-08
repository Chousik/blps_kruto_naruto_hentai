package ru.chousik.kt_blps.security

import ru.chousik.kt_blps.model.UserRole
import java.security.Principal
import java.util.*

data class XmlAccountDefinition(
    val username: String,
    val passwordHash: String,
    val userId: UUID,
    val role: UserRole
) {
    fun toPrincipal(): Principal {
        return XmlAccountPrincipal(
            username = username,
            userId = userId,
            role = role
        )
    }
}
