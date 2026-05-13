package ru.chousik.payment_worker_service.service

import java.time.OffsetDateTime
import java.util.UUID
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.PageRequest
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate
import ru.chousik.payment_worker_service.model.PaymentOutboxEvent
import ru.chousik.payment_worker_service.model.PaymentOutboxEventStatus
import ru.chousik.payment_worker_service.repository.PaymentOutboxEventRepository

@Service
class PaymentOutboxPublisher(
    private val paymentOutboxEventRepository: PaymentOutboxEventRepository,
    private val kafkaTemplate: KafkaTemplate<String, String>,
    private val transactionTemplate: TransactionTemplate,
    @Value("\${app.kafka.payment-url-assigned-topic}")
    private val paymentUrlAssignedTopic: String,
    @Value("\${app.outbox.batch-size:50}")
    private val batchSize: Int,
    @Value("\${app.outbox.max-attempts:10}")
    private val maxAttempts: Int,
    @Value("\${app.outbox.initial-retry-delay-ms:5000}")
    private val initialRetryDelayMs: Long,
    @Value("\${app.outbox.max-retry-delay-ms:300000}")
    private val maxRetryDelayMs: Long
) {

    @Scheduled(fixedDelayString = "\${app.outbox.fixed-delay-ms:5000}")
    fun publishPendingEvents() {
        claimBatch().forEach(::publishSingle)
    }

    fun publishNow(eventId: UUID) = claimEvent(eventId)?.let(::publishSingle)

    private fun claimBatch(): List<PaymentOutboxEvent> =
        transactionTemplate.execute {
            val now = OffsetDateTime.now()
            paymentOutboxEventRepository.findReadyBatch(
                statuses = listOf(PaymentOutboxEventStatus.PENDING, PaymentOutboxEventStatus.FAILED),
                attemptedBefore = now,
                maxAttempts = maxAttempts,
                pageable = PageRequest.of(0, batchSize)
            ).map { claim(it, now) }
        }.orEmpty()

    private fun claimEvent(eventId: UUID): PaymentOutboxEvent? =
        transactionTemplate.execute {
            val event = paymentOutboxEventRepository.findByIdForUpdate(eventId) ?: return@execute null
            val now = OffsetDateTime.now()
            if (!event.isReadyForAttempt(now, maxAttempts)) {
                return@execute null
            }

            claim(event, now)
        }

    private fun claim(event: PaymentOutboxEvent, now: OffsetDateTime): PaymentOutboxEvent {
        event.attemptCount += 1
        event.attemptedAt = now.plus(computeBackoff(event.attemptCount))
        return paymentOutboxEventRepository.save(event)
    }

    private fun publishSingle(event: PaymentOutboxEvent) {
        try {
            kafkaTemplate.send(paymentUrlAssignedTopic, event.id.toString(), event.payload).get()
            markSent(event.id)
        } catch (ex: Exception) {
            markFailed(event.id, ex)
        }
    }

    private fun markSent(eventId: UUID) {
        transactionTemplate.executeWithoutResult {
            val event = paymentOutboxEventRepository.findById(eventId).orElse(null) ?: return@executeWithoutResult
            if (event.status == PaymentOutboxEventStatus.SENT) {
                return@executeWithoutResult
            }

            event.status = PaymentOutboxEventStatus.SENT
            event.attemptedAt = OffsetDateTime.now()
            event.lastError = null
            paymentOutboxEventRepository.save(event)
        }
    }

    private fun markFailed(eventId: UUID, exception: Exception) {
        transactionTemplate.executeWithoutResult {
            val event = paymentOutboxEventRepository.findById(eventId).orElse(null) ?: return@executeWithoutResult
            if (event.status == PaymentOutboxEventStatus.SENT) {
                return@executeWithoutResult
            }

            val now = OffsetDateTime.now()
            event.status = PaymentOutboxEventStatus.FAILED
            event.attemptedAt = now.plus(computeBackoff(event.attemptCount))
            event.lastError = exception.message?.take(2000) ?: exception.javaClass.simpleName
            paymentOutboxEventRepository.save(event)
        }
    }

    private fun computeBackoff(attemptCount: Int): java.time.Duration {
        val multiplier = 1L shl (attemptCount - 1).coerceAtMost(20)
        val delayMs = (initialRetryDelayMs * multiplier).coerceAtMost(maxRetryDelayMs)
        return java.time.Duration.ofMillis(delayMs)
    }

    private fun PaymentOutboxEvent.isReadyForAttempt(now: OffsetDateTime, maxAttempts: Int) =
        status != PaymentOutboxEventStatus.SENT &&
            attemptCount < maxAttempts &&
            attemptedAt <= now
}
