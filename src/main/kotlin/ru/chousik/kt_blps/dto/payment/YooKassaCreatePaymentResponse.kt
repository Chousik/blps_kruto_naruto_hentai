package ru.chousik.kt_blps.dto.payment

class YooKassaCreatePaymentResponse(
    val id: String,
    val status: String,
    val confirmation: Confirmation?
) {
    data class Confirmation(
        val type: String?,
        val confirmation_url: String?
    )
}