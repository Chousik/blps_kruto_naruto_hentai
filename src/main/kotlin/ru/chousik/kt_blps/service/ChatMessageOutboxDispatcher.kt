package ru.chousik.blps_kt.service

import com.fasterxml.jackson.databind.json.JsonMapper
import java.time.OffsetDateTime
import java.util.UUID
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate
import ru.chousik.blps_kt.api.chat.ChatMessageResponse
import ru.chousik.blps_kt.model.ChatMessageOutboxStatus
import ru.chousik.blps_kt.repository.ChatMessageOutboxRepository

@Service
class ChatMessageOutboxDispatcher(
    private val chatMessageOutboxRepository: ChatMessageOutboxRepository,
    private val messagingTemplate: SimpMessagingTemplate,
    @Qualifier("jtaTransactionTemplate")
    private val transactionTemplate: TransactionTemplate,
    @Qualifier("jtaRequiresNewTransactionTemplate")
    private val requiresNewTransactionTemplate: TransactionTemplate
) {

    private val objectMapper = JsonMapper.builder()
        .findAndAddModules()
        .build()

    @Scheduled(fixedDelayString = "\${chat.outbox.dispatch-delay-ms:1000}")
    fun dispatchPending() {
        val pendingIds = transactionTemplate.execute {
            chatMessageOutboxRepository
                .findTop50ByStatusAndAvailableAtLessThanEqualOrderByCreatedAtAsc(
                    ChatMessageOutboxStatus.PENDING,
                    OffsetDateTime.now()
                )
                .map { it.id }
        }.orEmpty()

        pendingIds.forEach(::dispatchOne)
    }

    private fun dispatchOne(outboxId: UUID) {
        val record = transactionTemplate.execute {
            chatMessageOutboxRepository.findById(outboxId).orElse(null)
        } ?: return

        if (record.status != ChatMessageOutboxStatus.PENDING || record.availableAt.isAfter(OffsetDateTime.now())) {
            return
        }

        try {
            val payload = objectMapper.readValue(record.payload, ChatMessageResponse::class.java)
            messagingTemplate.convertAndSend(record.destination, payload)
            markSent(outboxId)
        } catch (ex: Exception) {
            scheduleRetry(outboxId, ex)
        }
    }

    private fun markSent(outboxId: UUID) {
        requiresNewTransactionTemplate.executeWithoutResult {
            val record = chatMessageOutboxRepository.findById(outboxId).orElse(null)
                ?: return@executeWithoutResult
            val now = OffsetDateTime.now()
            record.status = ChatMessageOutboxStatus.SENT
            record.attempts += 1
            record.lastAttemptAt = now
            record.sentAt = now
            record.lastError = null
            chatMessageOutboxRepository.save(record)
        }
    }

    private fun scheduleRetry(outboxId: UUID, ex: Exception) {
        requiresNewTransactionTemplate.executeWithoutResult {
            val record = chatMessageOutboxRepository.findById(outboxId).orElse(null)
                ?: return@executeWithoutResult
            val now = OffsetDateTime.now()
            val retryDelaySeconds = minOf(60L, 1L shl record.attempts.coerceAtMost(5))

            record.attempts += 1
            record.lastAttemptAt = now
            record.availableAt = now.plusSeconds(retryDelaySeconds)
            record.lastError = ex.message?.take(1000) ?: ex.javaClass.simpleName
            chatMessageOutboxRepository.save(record)
        }
    }
}
