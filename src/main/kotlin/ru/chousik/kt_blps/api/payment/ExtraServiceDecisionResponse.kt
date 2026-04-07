package ru.chousik.kt_blps.api.payment

import ru.chousik.kt_blps.api.extraservice.ExtraServiceRequestResponseDTO

data class ExtraServiceDecisionResponse(
    val extraService: ExtraServiceRequestResponseDTO,
    val payment: PaymentRequestView? = null
)
