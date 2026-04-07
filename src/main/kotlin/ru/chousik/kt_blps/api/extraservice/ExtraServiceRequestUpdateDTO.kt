package ru.chousik.blps_kt.api.extraservice

import jakarta.validation.constraints.Digits
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import ru.chousik.blps_kt.model.ExtraServiceRequestStatus

/**
 * DTO for partial updates of an extra service request.
 */
data class ExtraServiceRequestUpdateDTO(
    @field:Size(max = 255)
    val title: String? = null,

    @field:Size(max = 2000)
    val description: String? = null,

    @field:Positive
    @field:Digits(integer = 10, fraction = 2)
    val amount: BigDecimal? = null,

    @field:Pattern(regexp = "^[A-Z]{3}$")
    val currency: String? = null,

    val status: ExtraServiceRequestStatus? = null
)
