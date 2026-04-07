package ru.chousik.blps_kt.api.extraservice

import jakarta.validation.constraints.Digits
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import java.math.BigDecimal

data class ExtraServiceRequestCreateDTO(
    @field:NotBlank
    @field:Size(max = 255)
    val title: String?,

    @field:NotBlank
    @field:Size(max = 2000)
    val description: String?,

    @field:NotNull
    @field:Positive
    @field:Digits(integer = 10, fraction = 2)
    val amount: BigDecimal?,

    @field:NotBlank
    @field:Pattern(regexp = "^[A-Z]{3}$")
    val currency: String?
)
