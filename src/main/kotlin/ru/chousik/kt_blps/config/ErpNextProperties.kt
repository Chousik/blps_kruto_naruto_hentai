package ru.chousik.kt_blps.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@ConfigurationProperties(prefix = "erpnext")
@Component
data class ErpNextProperties(
    var company: String = "",
    var serviceItemCode: String = "",
    var jndiName: String = ""
)
