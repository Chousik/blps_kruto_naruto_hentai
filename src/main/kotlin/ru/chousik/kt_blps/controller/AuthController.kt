package ru.chousik.blps_kt.controller

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler
import org.springframework.security.web.context.SecurityContextRepository
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import ru.chousik.blps_kt.api.security.CurrentUserResponse
import ru.chousik.blps_kt.api.security.RegisterAccountRequest
import ru.chousik.blps_kt.api.security.RegisteredAccountResponse
import ru.chousik.blps_kt.security.CurrentAccountService
import ru.chousik.blps_kt.service.AccountRegistrationService

@RestController
@RequestMapping("/auth")
class AuthController(
    private val currentAccountService: CurrentAccountService,
    private val accountRegistrationService: AccountRegistrationService,
    private val securityContextRepository: SecurityContextRepository
) {

    @PostMapping("/session")
    fun createSession(
        authentication: Authentication,
        request: HttpServletRequest,
        response: HttpServletResponse
    ): CurrentUserResponse {
        val context = SecurityContextHolder.createEmptyContext()
        context.authentication = authentication
        SecurityContextHolder.setContext(context)
        securityContextRepository.saveContext(context, request, response)
        return CurrentUserResponse.from(currentAccountService.fromAuthentication(authentication))
    }

    @PostMapping("/logout")
    fun logout(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authentication: Authentication?
    ): ResponseEntity<Void> {
        SecurityContextLogoutHandler().logout(request, response, authentication)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    fun register(@Valid @RequestBody request: RegisterAccountRequest): RegisteredAccountResponse =
        accountRegistrationService.register(request)
}
