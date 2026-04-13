package ru.chousik.kt_blps.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@ConfigurationProperties(prefix = "yookassa")
@Component
data class YooKassaProperties(
        var shopId: String = "",
        var secretKey: String = "",
        var apiUrl: String = "",
        var returnUrl: String = ""
)

