package ru.chousik.blps_kt.api.payment

import ru.chousik.blps_kt.api.extraservice.ExtraServiceRequestResponseDTO

data class ExtraServiceDecisionResponse(
    val extraService: ExtraServiceRequestResponseDTO,
    val payment: PaymentRequestView? = null
)
