package ru.chousik.payment_worker_service.service

import com.fasterxml.jackson.databind.ObjectMapper
import java.math.RoundingMode
import java.time.OffsetDateTime
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.PageRequest
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate
import ru.chousik.payment_worker_service.dto.payment.YooKassaCreatePaymentRequest
import ru.chousik.payment_worker_service.event.PaymentCreationRequestedEvent
import ru.chousik.payment_worker_service.model.PaymentCreationSaga
import ru.chousik.payment_worker_service.model.PaymentCreationSagaStatus
import ru.chousik.payment_worker_service.model.PaymentRequest
import ru.chousik.payment_worker_service.model.PaymentRequestStatus
import ru.chousik.payment_worker_service.repository.PaymentCreationSagaRepository
import ru.chousik.payment_worker_service.repository.PaymentRequestRepository

@Service
class PaymentRequestConsumerService(
    private val objectMapper: ObjectMapper,
    private val paymentRequestRepository: PaymentRequestRepository,
    private val paymentCreationSagaRepository: PaymentCreationSagaRepository,
    private val paymentOutboxService: PaymentOutboxService,
    private val yooKassaClient: YooKassaClient,
    private val transactionTemplate: TransactionTemplate,
    @Value("\${app.saga.batch-size:20}")
    private val sagaBatchSize: Int,
    @Value("\${app.saga.max-attempts:3}")
    private val sagaMaxAttempts: Int,
    @Value("\${app.saga.initial-retry-delay-ms:30000}")
    private val sagaInitialRetryDelayMs: Long,
    @Value("\${app.saga.max-retry-delay-ms:300000}")
    private val sagaMaxRetryDelayMs: Long
) {
    @KafkaListener(topics = ["\${app.kafka.payment-topic}"])
    fun consume(payload: String) {
        val event = objectMapper.readValue(payload, PaymentCreationRequestedEvent::class.java)
        processWithFailureTracking(event)
    }

    @Scheduled(fixedDelayString = "\${app.saga.fixed-delay-ms:30000}")
    fun retryFailedSagas() {
        val sagas = transactionTemplate.execute {
            paymentCreationSagaRepository.findRetryableSagas(
                statuses = listOf(
                    PaymentCreationSagaStatus.FAILED,
                    PaymentCreationSagaStatus.PROCESSING
                ),
                attemptedBefore = OffsetDateTime.now(),
                maxAttempts = sagaMaxAttempts,
                pageable = PageRequest.of(0, sagaBatchSize)
            )
        }

        for (saga in sagas) {
            processWithFailureTracking(toEvent(saga))
        }
    }

    private fun processWithFailureTracking(event: PaymentCreationRequestedEvent) {
        try {
            process(event)
        } catch (ex: Exception) {
            markSagaFailed(event, ex)
        }
    }

    private fun process(event: PaymentCreationRequestedEvent) {
        when (beginSagaAttempt(event)) {
            SagaAttemptResult.COMPLETED,
            SagaAttemptResult.EXHAUSTED -> return

            SagaAttemptResult.READY -> Unit
        }

        val existingPayment = transactionTemplate.execute {
            paymentRequestRepository.findByExtraServiceRequestId(event.extraServiceRequestId)
        }
        if (existingPayment != null) {
            completeSagaWithExistingPayment(event.extraServiceRequestId, existingPayment)
            return
        }

        val response = yooKassaClient.createPayment(
            event.extraServiceRequestId.toString(),
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
                    "chatId" to event.chatId.toString(),
                    "initiatedByUserId" to event.initiatedByUserId.toString()
                )
            )
        )

        val confirmationUrl = response.confirmation?.confirmation_url
            ?: throw IllegalStateException("YooKassa did not return confirmation url")

        persistSuccessfulPayment(event, response.id, confirmationUrl)
    }

    private fun beginSagaAttempt(event: PaymentCreationRequestedEvent): SagaAttemptResult =
        transactionTemplate.execute {
            val now = OffsetDateTime.now()
            val existingPayment = paymentRequestRepository.findByExtraServiceRequestId(event.extraServiceRequestId)
            val saga = paymentCreationSagaRepository.findByExtraServiceRequestId(event.extraServiceRequestId)
                ?: paymentCreationSagaRepository.save(newSaga(event, now))

            if (existingPayment != null && !existingPayment.paymentUrl.isNullOrBlank()) {
                saga.paymentRequestId = existingPayment.id
                saga.providerPaymentId = existingPayment.providerPaymentId
                saga.paymentUrl = existingPayment.paymentUrl
                saga.status = PaymentCreationSagaStatus.COMPLETED
                saga.errorMessage = null
                saga.attemptedAt = now
                saga.updatedAt = now
                paymentCreationSagaRepository.save(saga)
                return@execute SagaAttemptResult.COMPLETED
            }

            if (saga.status == PaymentCreationSagaStatus.FAILED && saga.attemptCount >= sagaMaxAttempts) {
                return@execute SagaAttemptResult.EXHAUSTED
            }

            saga.status = PaymentCreationSagaStatus.PROCESSING
            saga.errorMessage = null
            saga.attemptedAt = now
            saga.updatedAt = now
            paymentCreationSagaRepository.save(saga)
            SagaAttemptResult.READY
        }

    private fun persistSuccessfulPayment(
        event: PaymentCreationRequestedEvent,
        providerPaymentId: String,
        paymentUrl: String
    ) {
        transactionTemplate.executeWithoutResult {
            val now = OffsetDateTime.now()
            val payment = paymentRequestRepository.findByExtraServiceRequestId(event.extraServiceRequestId)
                ?: PaymentRequest().apply {
                    extraServiceRequestId = event.extraServiceRequestId
                    initiatedByUserId = event.initiatedByUserId
                    createdAt = now
                }

            payment.providerPaymentId = providerPaymentId
            payment.paymentUrl = paymentUrl
            payment.status = PaymentRequestStatus.PENDING
            payment.expiresAt = now.plusHours(1)
            payment.resolvedAt = null
            val savedPayment = paymentRequestRepository.save(payment)

            val saga = paymentCreationSagaRepository.findByExtraServiceRequestId(event.extraServiceRequestId)
                ?: paymentCreationSagaRepository.save(newSaga(event, now))
            saga.paymentRequestId = savedPayment.id
            saga.providerPaymentId = providerPaymentId
            saga.paymentUrl = paymentUrl
            saga.errorMessage = null
            saga.attemptCount = 0
            saga.status = PaymentCreationSagaStatus.COMPLETED
            saga.attemptedAt = now
            saga.updatedAt = now
            paymentCreationSagaRepository.save(saga)

            paymentOutboxService.enqueuePaymentUrlAssignedEvent(savedPayment.id, event.extraServiceRequestId)
        }
    }

    private fun completeSagaWithExistingPayment(extraServiceRequestId: java.util.UUID, payment: PaymentRequest) {
        transactionTemplate.executeWithoutResult {
            val now = OffsetDateTime.now()
            val saga = paymentCreationSagaRepository.findByExtraServiceRequestId(extraServiceRequestId)
                ?: return@executeWithoutResult

            saga.paymentRequestId = payment.id
            saga.providerPaymentId = payment.providerPaymentId
            saga.paymentUrl = payment.paymentUrl
            saga.errorMessage = null
            saga.attemptCount = 0
            saga.status = PaymentCreationSagaStatus.COMPLETED
            saga.attemptedAt = now
            saga.updatedAt = now
            paymentCreationSagaRepository.save(saga)

            if (!payment.paymentUrl.isNullOrBlank()) {
                paymentOutboxService.enqueuePaymentUrlAssignedEvent(payment.id, extraServiceRequestId)
            }
        }
    }

    private fun markSagaFailed(event: PaymentCreationRequestedEvent, exception: Exception) {
        transactionTemplate.executeWithoutResult {
            val now = OffsetDateTime.now()
            val saga = paymentCreationSagaRepository.findByExtraServiceRequestId(event.extraServiceRequestId)
                ?: paymentCreationSagaRepository.save(newSaga(event, now))
            val nextAttemptCount = saga.attemptCount + 1

            saga.attemptCount = nextAttemptCount
            saga.status = PaymentCreationSagaStatus.FAILED
            saga.errorMessage = exception.stackTraceToString().take(4000)
            saga.attemptedAt = now.plus(computeBackoff(nextAttemptCount))
            saga.updatedAt = now
            paymentCreationSagaRepository.save(saga)

            if (nextAttemptCount >= sagaMaxAttempts) {
                paymentOutboxService.enqueuePaymentCreationFailedEvent(event.extraServiceRequestId, exception.message)
            }
        }
    }

    private fun newSaga(event: PaymentCreationRequestedEvent, now: OffsetDateTime) =
        PaymentCreationSaga().apply {
            extraServiceRequestId = event.extraServiceRequestId
            chatId = event.chatId
            initiatedByUserId = event.initiatedByUserId
            title = event.title.trim()
            amount = event.amount.toBigDecimal().setScale(2, RoundingMode.HALF_UP)
            currency = event.currency.uppercase()
            attemptCount = 0
            status = PaymentCreationSagaStatus.PENDING
            attemptedAt = now
            createdAt = now
            updatedAt = now
        }

    private fun toEvent(saga: PaymentCreationSaga) =
        PaymentCreationRequestedEvent(
            extraServiceRequestId = saga.extraServiceRequestId,
            chatId = saga.chatId,
            initiatedByUserId = saga.initiatedByUserId,
            title = saga.title,
            amount = saga.amount.setScale(2, RoundingMode.HALF_UP).toPlainString(),
            currency = saga.currency
        )

    private fun computeBackoff(attemptCount: Int): java.time.Duration {
        val multiplier = 1L shl (attemptCount - 1).coerceAtMost(20)
        val delayMs = (sagaInitialRetryDelayMs * multiplier).coerceAtMost(sagaMaxRetryDelayMs)
        return java.time.Duration.ofMillis(delayMs)
    }

    private enum class SagaAttemptResult {
        READY,
        COMPLETED,
        EXHAUSTED
    }
}
