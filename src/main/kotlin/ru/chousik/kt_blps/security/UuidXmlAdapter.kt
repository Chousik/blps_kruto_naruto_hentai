package ru.chousik.kt_blps.security

import jakarta.xml.bind.annotation.adapters.XmlAdapter
import java.util.UUID

class UuidXmlAdapter : XmlAdapter<String, UUID>() {
    override fun unmarshal(value: String?): UUID =
        value?.let(UUID::fromString) ?: UUID(0L, 0L)

    override fun marshal(value: UUID?): String =
        value?.toString().orEmpty()
}
