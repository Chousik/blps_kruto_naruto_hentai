package ru.chousik.kt_blps.dto.payment

data class YooKassaWebhookNotification(
    val type: String,
    val event: String,
    val `object`: YooKassaPaymentObject
)

data class YooKassaPaymentObject(
    val id: String,
    val status: String,
    val paid: Boolean? = null,
    val created_at: String? = null,
    val expires_at: String? = null,
    val description: String? = null,
    val metadata: Map<String, String>? = null
)