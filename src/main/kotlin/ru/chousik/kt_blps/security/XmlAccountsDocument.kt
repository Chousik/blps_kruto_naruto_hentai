package ru.chousik.kt_blps.security

import jakarta.xml.bind.annotation.XmlAccessType
import jakarta.xml.bind.annotation.XmlAccessorType
import jakarta.xml.bind.annotation.XmlElement
import jakarta.xml.bind.annotation.XmlRootElement

@XmlRootElement(name = "accounts")
@XmlAccessorType(XmlAccessType.FIELD)
data class XmlAccountsDocument(
    @field:XmlElement(name = "account")
    var accounts: MutableList<XmlAccountDefinition> = mutableListOf()
)
