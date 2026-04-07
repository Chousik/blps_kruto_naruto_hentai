package ru.chousik.blps_kt.api.payment

import jakarta.validation.constraints.NotNull

data class ExtraServiceDecisionRequest(
    @field:NotNull
    var decision: ExtraServiceDecision?
)
