package ru.chousik.kt_blps.dto.payment

data class YooKassaCreatePaymentRequest(
    val amount: Amount,
    val capture: Boolean,
    val confirmation: Confirmation,
    val description: String,
    val metadata: Map<String, String>
) {
    data class Amount(
        val value: String,
        val currency: String
    )

    data class Confirmation(
        val type: String,
        val return_url: String
    )
}