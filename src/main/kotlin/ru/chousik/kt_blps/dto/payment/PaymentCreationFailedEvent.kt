package ru.chousik.kt_blps.dto.payment

import java.util.UUID

data class PaymentCreationFailedEvent(
    val extraServiceRequestId: UUID,
    val errorMessage: String?
)
