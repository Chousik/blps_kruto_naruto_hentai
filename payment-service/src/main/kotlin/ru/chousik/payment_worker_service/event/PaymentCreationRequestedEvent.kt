package ru.chousik.payment_worker_service.event

import java.util.UUID

data class PaymentCreationRequestedEvent(
    val extraServiceRequestId: UUID,
    val chatId: UUID,
    val initiatedByUserId: UUID,
    val title: String,
    val amount: String,
    val currency: String
)
