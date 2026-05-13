package ru.chousik.kt_blps.dto.payment

import java.util.UUID

data class PaymentCreationRequestedEvent(
    val extraServiceRequestId: UUID,
    val chatId: UUID,
    val initiatedByUserId: UUID,
    val title: String,
    val amount: String,
    val currency: String
)
