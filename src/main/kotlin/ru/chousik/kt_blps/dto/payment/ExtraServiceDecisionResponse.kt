package ru.chousik.kt_blps.dto.payment

import ru.chousik.kt_blps.dto.extraservice.ExtraServiceRequestResponseDTO

data class ExtraServiceDecisionResponse(
    val extraService: ExtraServiceRequestResponseDTO,
    val payment: PaymentRequestView? = null
)
