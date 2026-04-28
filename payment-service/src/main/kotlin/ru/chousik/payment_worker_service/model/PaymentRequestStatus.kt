package ru.chousik.payment_worker_service.model

enum class PaymentRequestStatus {
    WAITING_PAYMENT,
    PENDING,
    PAID,
    FAILED
}
