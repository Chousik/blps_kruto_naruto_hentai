package ru.chousik.kt_blps.api.payment

import jakarta.validation.constraints.NotNull

data class ExtraServiceDecisionRequest(
    @field:NotNull
    var decision: ExtraServiceDecision?
)
