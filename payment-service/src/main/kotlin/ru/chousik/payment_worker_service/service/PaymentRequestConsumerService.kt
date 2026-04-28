package ru.chousik.payment_worker_service.service

import com.fasterxml.jackson.databind.ObjectMapper
import java.time.OffsetDateTime
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.chousik.payment_worker_service.dto.payment.YooKassaCreatePaymentRequest
import ru.chousik.payment_worker_service.event.PaymentRequestCreatedEvent
import ru.chousik.payment_worker_service.model.PaymentRequestStatus
import ru.chousik.payment_worker_service.repository.PaymentRequestRepository

@Service
class PaymentRequestConsumerService(
    private val objectMapper: ObjectMapper,
    private val paymentRequestRepository: PaymentRequestRepository,
    private val yooKassaClient: YooKassaClient
) {

    @KafkaListener(topics = ["\${app.kafka.payment-topic}"])
    @Transactional
    fun consume(payload: String) {
        val event = objectMapper.readValue(payload, PaymentRequestCreatedEvent::class.java)
        val payment = paymentRequestRepository.findById(event.paymentRequestId)
            .orElseThrow { IllegalStateException("payment request not found") }

        if (!payment.providerPaymentId.isNullOrBlank()) {
            return
        }

        val response = yooKassaClient.createPayment(
            YooKassaCreatePaymentRequest(
                amount = YooKassaCreatePaymentRequest.Amount(
                    value = event.amount,
                    currency = event.currency
                ),
                capture = true,
                confirmation = YooKassaCreatePaymentRequest.Confirmation(
                    type = "redirect",
                    return_url = yooKassaClient.returnUrl()
                ),
                description = event.title,
                metadata = mapOf(
                    "extraServiceRequestId" to event.extraServiceRequestId.toString(),
                    "paymentRequestId" to event.paymentRequestId.toString(),
                    "chatId" to event.chatId.toString()
                )
            )
        )

        val confirmationUrl = response.confirmation?.confirmation_url
            ?: throw IllegalStateException("YooKassa did not return confirmation url")

        payment.providerPaymentId = response.id
        payment.paymentUrl = confirmationUrl
        payment.status = PaymentRequestStatus.PENDING
        payment.expiresAt = OffsetDateTime.now().plusHours(1)
        payment.resolvedAt = null
        paymentRequestRepository.save(payment)
    }
}
