package ru.chousik.kt_blps.dto.auth

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import ru.chousik.kt_blps.model.UserRole

data class RegisterRequest(
    @field:NotBlank
    @field:Size(min = 3, max = 128)
    @field:Pattern(regexp = "^[A-Za-z0-9._-]+$")
    val username: String?,

    @field:NotBlank
    @field:Email
    @field:Size(max = 255)
    val email: String?,

    @field:NotBlank
    @field:Size(min = 8, max = 72)
    val password: String?,

    @field:NotBlank
    @field:Size(max = 128)
    val firstName: String?,

    @field:NotBlank
    @field:Size(max = 128)
    val lastName: String?,

    @field:NotNull
    val role: UserRole?
)
