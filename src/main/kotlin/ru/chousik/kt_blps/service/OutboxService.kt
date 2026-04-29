package ru.chousik.kt_blps.service

import java.time.OffsetDateTime
import java.util.UUID
import org.springframework.stereotype.Service
import ru.chousik.kt_blps.model.OutboxEvent
import ru.chousik.kt_blps.model.OutboxEventStatus
import ru.chousik.kt_blps.repository.OutboxEventRepository

@Service
class OutboxService(
    private val outboxEventRepository: OutboxEventRepository
) {

    fun enqueue(
        aggregateType: String,
        aggregateId: UUID,
        eventType: String,
        topic: String,
        messageKey: String,
        payload: String
    ) {
        val now = OffsetDateTime.now()
        outboxEventRepository.save(
            OutboxEvent().apply {
                this.aggregateType = aggregateType
                this.aggregateId = aggregateId
                this.eventType = eventType
                this.topic = topic
                this.messageKey = messageKey
                this.payload = payload
                status = OutboxEventStatus.PENDING
                createdAt = now
                updatedAt = now
            }
        )
    }
}
