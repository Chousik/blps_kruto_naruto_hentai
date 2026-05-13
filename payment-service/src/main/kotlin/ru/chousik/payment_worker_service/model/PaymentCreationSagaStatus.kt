package ru.chousik.payment_worker_service.model

enum class PaymentCreationSagaStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED
}
