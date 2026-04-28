package ru.chousik.payment_worker_service.event

import java.util.UUID

data class PaymentRequestCreatedEvent(
    val paymentRequestId: UUID,
    val extraServiceRequestId: UUID,
    val chatId: UUID,
    val title: String,
    val amount: String,
    val currency: String
)
