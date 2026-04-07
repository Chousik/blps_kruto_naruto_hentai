package ru.chousik.blps_kt.repository

import java.time.OffsetDateTime
import java.util.UUID
import org.springframework.data.jpa.repository.JpaRepository
import ru.chousik.blps_kt.model.ChatMessageOutbox
import ru.chousik.blps_kt.model.ChatMessageOutboxStatus

interface ChatMessageOutboxRepository : JpaRepository<ChatMessageOutbox, UUID> {
    fun findTop50ByStatusAndAvailableAtLessThanEqualOrderByCreatedAtAsc(
        status: ChatMessageOutboxStatus,
        availableAt: OffsetDateTime
    ): List<ChatMessageOutbox>
}

