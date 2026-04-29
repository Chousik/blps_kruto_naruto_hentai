package ru.chousik.kt_blps.dto.payment

import java.util.UUID

data class PaymentUrlAssignedEvent(
    val paymentRequestId: UUID,
    val extraServiceRequestId: UUID
)
