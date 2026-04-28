package ru.chousik.payment_worker_service.controller

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import ru.chousik.payment_worker_service.service.PaymentWebhookService

@RestController
@RequestMapping("/payments/yookassa")
class YooKassaWebhookController(
    private val paymentWebhookService: PaymentWebhookService
) {
    @PostMapping("/webhook")
    fun handleWebhook(@RequestBody payload: String): ResponseEntity<Unit> {
        paymentWebhookService.handleWebhook(payload)
        return ResponseEntity.ok().build()
    }
}
