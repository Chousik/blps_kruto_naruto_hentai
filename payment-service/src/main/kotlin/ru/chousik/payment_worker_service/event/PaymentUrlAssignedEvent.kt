package ru.chousik.payment_worker_service.event

import java.util.UUID

data class PaymentUrlAssignedEvent(
    val paymentRequestId: UUID,
    val extraServiceRequestId: UUID
)
