package ru.chousik.blps_kt.controller

import com.fasterxml.jackson.databind.json.JsonMapper
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import ru.chousik.blps_kt.service.payment.YooKassaNotificationProcessor
import ru.chousik.blps_kt.service.payment.YooKassaWebhookSecurityService

@RestController
@RequestMapping("/webhooks/yookassa")
class YooKassaWebhookController(
    private val notificationProcessor: YooKassaNotificationProcessor,
    private val webhookSecurityService: YooKassaWebhookSecurityService
) {

    private val log = LoggerFactory.getLogger(javaClass)
    private val objectMapper = JsonMapper.builder()
        .findAndAddModules()
        .build()

    @PostMapping
    fun handleWebhook(
        @RequestBody requestBody: String,
        request: HttpServletRequest
    ): ResponseEntity<Void> =
        try {
            webhookSecurityService.validateRemoteAddress(request.remoteAddr)
            val root = objectMapper.readTree(requestBody)
            notificationProcessor.process(root)
            ResponseEntity.ok().build()
        } catch (ex: ResponseStatusException) {
            log.warn("Rejected YooKassa webhook: {}", ex.reason)
            ResponseEntity.status(ex.statusCode).build()
        } catch (ex: Exception) {
            log.error("Failed to process YooKassa webhook", ex)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
}
