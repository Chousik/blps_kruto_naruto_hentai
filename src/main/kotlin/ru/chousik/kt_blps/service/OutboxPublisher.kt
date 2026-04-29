package ru.chousik.kt_blps.service

import java.time.Duration
import java.time.OffsetDateTime
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.PageRequest
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate
import ru.chousik.kt_blps.model.OutboxEvent
import ru.chousik.kt_blps.model.OutboxEventStatus
import ru.chousik.kt_blps.repository.OutboxEventRepository

@Service
class OutboxPublisher(
    private val outboxEventRepository: OutboxEventRepository,
    private val kafkaTemplate: KafkaTemplate<String, String>,
    @Qualifier("writeTransactionTemplate")
    private val writeTransactionTemplate: TransactionTemplate,
    @Value("\${app.outbox.batch-size:50}")
    private val batchSize: Int,
    @Value("\${app.outbox.lock-timeout-seconds:60}")
    lockTimeoutSeconds: Long
) {
    private val lockTimeout: Duration = Duration.ofSeconds(lockTimeoutSeconds)

    @Scheduled(fixedDelayString = "\${app.outbox.fixed-delay-ms:2000}")
    fun publishPendingEvents() {
        val batch = claimBatch()
        for (event in batch) {
            publishSingle(event)
        }
    }

    private fun claimBatch(): List<OutboxEvent> =
        writeTransactionTemplate.execute {
            val now = OffsetDateTime.now()
            val events = outboxEventRepository.findBatchForProcessing(
                pendingStatus = OutboxEventStatus.PENDING,
                inProgressStatus = OutboxEventStatus.IN_PROGRESS,
                staleLockThreshold = now.minus(lockTimeout),
                pageable = PageRequest.of(0, batchSize)
            )

            events.onEach { event ->
                event.status = OutboxEventStatus.IN_PROGRESS
                event.lockedAt = now
                event.updatedAt = now
                event.attemptCount += 1
            }
        }

    private fun publishSingle(event: OutboxEvent) {
        try {
            kafkaTemplate.send(event.topic, event.messageKey, event.payload).get()
            markPublished(event.id)
        } catch (ex: Exception) {
            markForRetry(event.id, ex)
        }
    }

    private fun markPublished(eventId: java.util.UUID) {
        writeTransactionTemplate.executeWithoutResult {
            val event = outboxEventRepository.findById(eventId).orElse(null) ?: return@executeWithoutResult
            if (event.status == OutboxEventStatus.PUBLISHED) {
                return@executeWithoutResult
            }

            val now = OffsetDateTime.now()
            event.status = OutboxEventStatus.PUBLISHED
            event.publishedAt = now
            event.lockedAt = null
            event.lastError = null
            event.updatedAt = now
        }
    }

    private fun markForRetry(eventId: java.util.UUID, exception: Exception) {
        writeTransactionTemplate.executeWithoutResult {
            val event = outboxEventRepository.findById(eventId).orElse(null) ?: return@executeWithoutResult
            if (event.status == OutboxEventStatus.PUBLISHED) {
                return@executeWithoutResult
            }

            val now = OffsetDateTime.now()
            event.status = OutboxEventStatus.PENDING
            event.lockedAt = null
            event.lastError = exception.message?.take(2000) ?: exception.javaClass.simpleName
            event.updatedAt = now
        }
    }
}
