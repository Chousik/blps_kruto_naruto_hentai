package ru.chousik.kt_blps.controller

import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import java.util.UUID
import org.springframework.http.HttpStatus
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
import ru.chousik.kt_blps.service.ExtraServiceRequestService

@RestController
@Validated
@RequestMapping
class ExtraServiceRequestController(
    private val extraServiceRequestService: ExtraServiceRequestService
) {

    @PostMapping("/extra-services")
    fun createExtraService(
        @RequestParam("requesterId") requesterId: UUID,
        @RequestParam("chatId") chatId: UUID,
        @Valid @RequestBody dto: ExtraServiceRequestCreateDTO
    ): ExtraServiceRequestResponseDTO =
        extraServiceRequestService.createExtraService(chatId, requesterId, dto)

    @GetMapping("/extra-services")
    fun getChatExtraServices(
        @RequestParam("requesterId") requesterId: UUID,
        @RequestParam("chatId") chatId: UUID,
        @RequestParam("limit", defaultValue = "20") @Min(1) @Max(100) limit: Int,
        @RequestParam("offset", defaultValue = "0") @Min(0) offset: Long
    ): PagedExtraServiceRequestsResponse {
        val page = extraServiceRequestService.getExtraServicesForChat(chatId, requesterId, limit, offset)
        return PagedExtraServiceRequestsResponse(
            items = page.content,
            total = page.totalElements,
            limit = limit,
            offset = offset
        )
    }

    @GetMapping("/extra-services/{serviceId}")
    fun getExtraService(
        @PathVariable serviceId: UUID,
        @RequestParam("requesterId") requesterId: UUID
    ): ExtraServiceRequestResponseDTO =
        extraServiceRequestService.getExtraService(serviceId, requesterId)

    @GetMapping("/extra-services/{serviceId}/payment")
    fun getExtraServicePayment(
        @PathVariable serviceId: UUID,
        @RequestParam("requesterId") requesterId: UUID
    ): PaymentRequestView =
        extraServiceRequestService.getExtraServicePayment(serviceId, requesterId)

    @PutMapping("/extra-services/{serviceId}")
    fun updateExtraService(
        @PathVariable serviceId: UUID,
        @RequestParam("requesterId") requesterId: UUID,
        @Valid @RequestBody dto: ExtraServiceRequestUpdateDTO
    ): ExtraServiceRequestResponseDTO =
        extraServiceRequestService.updateExtraService(serviceId, requesterId, dto)

    @PostMapping("/extra-services/{serviceId}/decision")
    fun decideExtraService(
        @PathVariable serviceId: UUID,
        @RequestParam("requesterId") requesterId: UUID,
        @Valid @RequestBody request: ExtraServiceDecisionRequest
    ): ExtraServiceDecisionResponse =
        extraServiceRequestService.decideExtraService(serviceId, requesterId, request)

    @DeleteMapping("/extra-services/{serviceId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteExtraService(
        @PathVariable serviceId: UUID,
        @RequestParam("requesterId") requesterId: UUID
    ) {
        extraServiceRequestService.deleteExtraService(serviceId, requesterId)
    }
}
