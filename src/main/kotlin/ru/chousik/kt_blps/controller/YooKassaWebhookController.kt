package ru.chousik.kt_blps.controller

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import ru.chousik.kt_blps.service.PaymentWebhookService

@RestController
@RequestMapping("/payments/yookassa")
class YooKassaWebhookController(
    private val paymentWebhookService: PaymentWebhookService
) {
    @PostMapping("/webhook")
    fun handleWebhook(
        @RequestBody payload: String,
        @RequestHeader(value = "X-Forwarded-For", required = false) forwardedFor: String?,
        @RequestHeader(value = "X-Real-IP", required = false) realIp: String?
    ): ResponseEntity<Unit> {
        paymentWebhookService.handleWebhook(payload, forwardedFor, realIp)
        return ResponseEntity.ok().build()
    }
}