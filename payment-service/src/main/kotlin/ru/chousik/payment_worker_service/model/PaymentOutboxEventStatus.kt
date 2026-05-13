package ru.chousik.payment_worker_service.model

enum class PaymentOutboxEventStatus {
    PENDING,
    SENT,
    FAILED
}
