package ru.chousik.kt_blps.controller

import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import ru.chousik.kt_blps.dto.auth.RegisterRequest
import ru.chousik.kt_blps.dto.auth.RegisterResponse
import ru.chousik.kt_blps.service.RegistrationService

@RestController
@Validated
@RequestMapping("/auth")
class AuthController(
    private val registrationService: RegistrationService
) {

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    fun register(
        @Valid @RequestBody request: RegisterRequest
    ): RegisterResponse =
        registrationService.register(request)
}
