package ru.chousik.payment_worker_service.dto.payment

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
