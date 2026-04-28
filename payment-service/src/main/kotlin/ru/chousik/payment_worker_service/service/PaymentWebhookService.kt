package ru.chousik.payment_worker_service.service

import com.fasterxml.jackson.databind.ObjectMapper
import java.time.OffsetDateTime
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import ru.chousik.payment_worker_service.dto.payment.YooKassaWebhookNotification
import ru.chousik.payment_worker_service.model.PaymentRequestStatus
import ru.chousik.payment_worker_service.repository.PaymentRequestRepository

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

        paymentRequestRepository.save(payment)
    }
}
