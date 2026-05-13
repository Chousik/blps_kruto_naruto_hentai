package ru.chousik.payment_worker_service.event

import java.util.UUID

data class PaymentCreationFailedEvent(
    val extraServiceRequestId: UUID,
    val errorMessage: String?
)
