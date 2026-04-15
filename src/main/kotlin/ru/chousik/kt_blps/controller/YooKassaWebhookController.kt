package ru.chousik.kt_blps.controller

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import ru.chousik.kt_blps.service.PaymentWebhookService


@RestController
@RequestMapping("/api/payments")
class YooKassaWebhookController(
    private val paymentWebhookService: PaymentWebhookService
) {

    @PostMapping("/webhook")
    @ResponseStatus(HttpStatus.OK)
    fun handleWebhook(@RequestBody payload: String) {
        paymentWebhookService.handleWebhook(payload)
    }
}