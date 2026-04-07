package ru.chousik.blps_kt.controller

import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import java.util.UUID
import org.springframework.http.HttpStatus
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
import ru.chousik.blps_kt.api.payment.ExtraServiceDecisionRequest
import ru.chousik.blps_kt.api.payment.ExtraServiceDecisionResponse
import ru.chousik.blps_kt.api.payment.PaymentRequestView
import ru.chousik.blps_kt.api.extraservice.ExtraServiceRequestCreateDTO
import ru.chousik.blps_kt.api.extraservice.ExtraServiceRequestResponseDTO
import ru.chousik.blps_kt.api.extraservice.ExtraServiceRequestUpdateDTO
import ru.chousik.blps_kt.api.extraservice.PagedExtraServiceRequestsResponse
import ru.chousik.blps_kt.service.ExtraServiceRequestService

@RestController
@Validated
@RequestMapping
class ExtraServiceRequestController(
    private val extraServiceRequestService: ExtraServiceRequestService
) {

    @PostMapping("/extra-services")
    @PreAuthorize("hasAuthority('PRIV_EXTRA_SERVICE_CREATE')")
    fun createExtraService(
        @RequestParam("chatId") chatId: UUID,
        @Valid @RequestBody dto: ExtraServiceRequestCreateDTO
    ): ExtraServiceRequestResponseDTO =
        extraServiceRequestService.createExtraService(chatId, dto)

    @GetMapping("/extra-services")
    @PreAuthorize("hasAuthority('PRIV_EXTRA_SERVICE_READ')")
    fun getChatExtraServices(
        @RequestParam("chatId") chatId: UUID,
        @RequestParam("limit", defaultValue = "20") @Min(1) @Max(100) limit: Int,
        @RequestParam("offset", defaultValue = "0") @Min(0) offset: Long
    ): PagedExtraServiceRequestsResponse {
        val page = extraServiceRequestService.getExtraServicesForChat(chatId, limit, offset)
        return PagedExtraServiceRequestsResponse(
            items = page.content,
            total = page.totalElements,
            limit = limit,
            offset = offset
        )
    }

    @GetMapping("/extra-services/{serviceId}")
    @PreAuthorize("hasAuthority('PRIV_EXTRA_SERVICE_READ')")
    fun getExtraService(@PathVariable serviceId: UUID): ExtraServiceRequestResponseDTO =
        extraServiceRequestService.getExtraService(serviceId)

    @GetMapping("/extra-services/{serviceId}/payment")
    @PreAuthorize("hasAuthority('PRIV_EXTRA_SERVICE_PAYMENT_READ')")
    fun getExtraServicePayment(@PathVariable serviceId: UUID): PaymentRequestView =
        extraServiceRequestService.getExtraServicePayment(serviceId)

    @PutMapping("/extra-services/{serviceId}")
    @PreAuthorize("hasAuthority('PRIV_EXTRA_SERVICE_UPDATE')")
    fun updateExtraService(
        @PathVariable serviceId: UUID,
        @Valid @RequestBody dto: ExtraServiceRequestUpdateDTO
    ): ExtraServiceRequestResponseDTO =
        extraServiceRequestService.updateExtraService(serviceId, dto)

    @PostMapping("/extra-services/{serviceId}/decision")
    @PreAuthorize("hasAuthority('PRIV_EXTRA_SERVICE_DECIDE')")
    fun decideExtraService(
        @PathVariable serviceId: UUID,
        @Valid @RequestBody request: ExtraServiceDecisionRequest
    ): ExtraServiceDecisionResponse =
        extraServiceRequestService.decideExtraService(serviceId, request)

    @DeleteMapping("/extra-services/{serviceId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('PRIV_EXTRA_SERVICE_DELETE')")
    fun deleteExtraService(@PathVariable serviceId: UUID) {
        extraServiceRequestService.deleteExtraService(serviceId)
    }
}
