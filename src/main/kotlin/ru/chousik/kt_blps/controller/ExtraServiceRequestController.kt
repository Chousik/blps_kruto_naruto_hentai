package ru.chousik.kt_blps.controller

import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import java.util.UUID
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import ru.chousik.kt_blps.dto.extraservice.ExtraServiceRequestCreateDTO
import ru.chousik.kt_blps.dto.extraservice.ExtraServiceRequestResponseDTO
import ru.chousik.kt_blps.dto.extraservice.ExtraServiceRequestUpdateDTO
import ru.chousik.kt_blps.dto.extraservice.PagedExtraServiceRequestsResponse
import ru.chousik.kt_blps.dto.payment.ExtraServiceDecisionRequest
import ru.chousik.kt_blps.dto.payment.ExtraServiceDecisionResponse
import ru.chousik.kt_blps.dto.payment.PaymentRequestView
import ru.chousik.kt_blps.security.AuthenticatedAccount
import ru.chousik.kt_blps.service.ExtraServiceRequestService

@RestController
@Validated
@RequestMapping
class ExtraServiceRequestController(
    private val extraServiceRequestService: ExtraServiceRequestService
) {

    @PreAuthorize("hasAuthority('PRIV_EXTRA_SERVICE_CREATE')")
    @PostMapping("/extra-services")
    fun createExtraService(
        @AuthenticationPrincipal(errorOnInvalidType = true) authenticatedAccount: AuthenticatedAccount,
        @RequestParam("chatId") chatId: UUID,
        @Valid @RequestBody dto: ExtraServiceRequestCreateDTO
    ): ExtraServiceRequestResponseDTO =
        extraServiceRequestService.createExtraService(chatId, authenticatedAccount.userId, dto)

    @PreAuthorize("hasAuthority('PRIV_EXTRA_SERVICE_READ')")
    @GetMapping("/extra-services")
    fun getChatExtraServices(
        @AuthenticationPrincipal(errorOnInvalidType = true) authenticatedAccount: AuthenticatedAccount,
        @RequestParam("chatId") chatId: UUID,
        @RequestParam("limit", defaultValue = "20") @Min(1) @Max(100) limit: Int,
        @RequestParam("offset", defaultValue = "0") @Min(0) offset: Long
    ): PagedExtraServiceRequestsResponse {
        val page = extraServiceRequestService.getExtraServicesForChat(
            chatId,
            authenticatedAccount.userId,
            limit,
            offset
        )
        return PagedExtraServiceRequestsResponse(
            items = page.content,
            total = page.totalElements,
            limit = limit,
            offset = offset
        )
    }

    @PreAuthorize("hasAuthority('PRIV_EXTRA_SERVICE_READ')")
    @GetMapping("/extra-services/{serviceId}")
    fun getExtraService(
        @PathVariable serviceId: UUID,
        @AuthenticationPrincipal(errorOnInvalidType = true) authenticatedAccount: AuthenticatedAccount
    ): ExtraServiceRequestResponseDTO =
        extraServiceRequestService.getExtraService(serviceId, authenticatedAccount.userId)

    @PreAuthorize("hasAuthority('PRIV_EXTRA_SERVICE_PAYMENT_READ')")
    @GetMapping("/extra-services/{serviceId}/payment")
    fun getExtraServicePayment(
        @PathVariable serviceId: UUID,
        @AuthenticationPrincipal(errorOnInvalidType = true) authenticatedAccount: AuthenticatedAccount
    ): PaymentRequestView =
        extraServiceRequestService.getExtraServicePayment(serviceId, authenticatedAccount.userId)

    @PreAuthorize("hasAuthority('PRIV_EXTRA_SERVICE_UPDATE')")
    @PutMapping("/extra-services/{serviceId}")
    fun updateExtraService(
        @PathVariable serviceId: UUID,
        @AuthenticationPrincipal(errorOnInvalidType = true) authenticatedAccount: AuthenticatedAccount,
        @Valid @RequestBody dto: ExtraServiceRequestUpdateDTO
    ): ExtraServiceRequestResponseDTO =
        extraServiceRequestService.updateExtraService(serviceId, authenticatedAccount.userId, dto)

    @PreAuthorize("hasAuthority('PRIV_EXTRA_SERVICE_DECIDE')")
    @PostMapping("/extra-services/{serviceId}/decision")
    fun decideExtraService(
        @PathVariable serviceId: UUID,
        @AuthenticationPrincipal(errorOnInvalidType = true) authenticatedAccount: AuthenticatedAccount,
        @Valid @RequestBody request: ExtraServiceDecisionRequest
    ): ExtraServiceDecisionResponse =
        extraServiceRequestService.decideExtraService(serviceId, authenticatedAccount.userId, request)

    @PreAuthorize("hasAuthority('PRIV_EXTRA_SERVICE_DELETE')")
    @DeleteMapping("/extra-services/{serviceId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteExtraService(
        @PathVariable serviceId: UUID,
        @AuthenticationPrincipal(errorOnInvalidType = true) authenticatedAccount: AuthenticatedAccount
    ) {
        extraServiceRequestService.deleteExtraService(serviceId, authenticatedAccount.userId)
    }
}
