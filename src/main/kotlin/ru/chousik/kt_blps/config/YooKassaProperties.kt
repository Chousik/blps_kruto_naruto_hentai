package ru.chousik.blps_kt.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "yookassa")
data class YooKassaProperties(
    var apiBaseUrl: String = "https://api.yookassa.ru",
    var shopId: String = "",
    var secretKey: String = "",
    var returnUrl: String = "http://localhost:8080/payment/return",
    var webhookIpCheckEnabled: Boolean = true
)
