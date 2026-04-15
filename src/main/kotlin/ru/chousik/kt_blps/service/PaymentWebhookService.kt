package ru.chousik.kt_blps.service

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import ru.chousik.kt_blps.dto.payment.YooKassaWebhookNotification
import ru.chousik.kt_blps.model.PaymentRequestStatus
import ru.chousik.kt_blps.repository.PaymentRequestRepository
import java.time.OffsetDateTime


@Service
class PaymentWebhookService(
    private val objectMapper: ObjectMapper,
    private val paymentRequestRepository: PaymentRequestRepository
) {

    @Transactional
    fun handleWebhook(payload: String) {
        val notification = try {
            objectMapper.readValue(payload, YooKassaWebhookNotification::class.java)
        } catch (ex: Exception) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid webhook payload", ex)
        }

        if (notification.type != "notification") {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "unsupported webhook type")
        }

        val payment = paymentRequestRepository.findByProviderPaymentId(notification.`object`.id)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "payment request not found")

        when (notification.event) {
            "payment.succeeded" -> {
                payment.status = PaymentRequestStatus.PAID
                payment.resolvedAt = OffsetDateTime.now()
            }
            "payment.canceled" -> {
                payment.status = PaymentRequestStatus.FAILED
                payment.resolvedAt = OffsetDateTime.now()
            }
            "payment.waiting_for_capture" -> {
                payment.status = PaymentRequestStatus.PENDING
            }
            else -> return
        }
    }
}

