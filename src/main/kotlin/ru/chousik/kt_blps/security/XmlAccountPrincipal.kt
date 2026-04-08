package ru.chousik.kt_blps.security

import java.security.Principal
import java.util.UUID
import ru.chousik.kt_blps.model.UserRole

data class XmlAccountPrincipal(
    val username: String,
    val userId: UUID,
    val role: UserRole
) : Principal {
    override fun getName(): String = username
}
