package ru.chousik.kt_blps.security

import jakarta.xml.bind.annotation.XmlAccessType
import jakarta.xml.bind.annotation.XmlAccessorType
import jakarta.xml.bind.annotation.XmlAttribute
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter
import ru.chousik.kt_blps.model.UserRole
import java.security.Principal
import java.util.*

@XmlAccessorType(XmlAccessType.FIELD)
data class XmlAccountDefinition(
    @field:XmlAttribute(name = "username")
    var username: String = "",
    @field:XmlAttribute(name = "password-hash")
    var passwordHash: String = "",
    @field:XmlAttribute(name = "user-id")
    @field:XmlJavaTypeAdapter(UuidXmlAdapter::class)
    var userId: UUID = UUID(0L, 0L),
    @field:XmlAttribute(name = "role")
    var role: UserRole = UserRole.GUEST
) {
    fun toPrincipal(): Principal {
        return XmlAccountPrincipal(
            username = username,
            userId = userId,
            role = role
        )
    }
}
