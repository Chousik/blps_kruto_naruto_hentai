package ru.chousik.kt_blps.repository

import java.time.OffsetDateTime
import java.util.UUID
import org.springframework.data.jpa.repository.JpaRepository
import ru.chousik.kt_blps.model.ChatMessageOutbox
import ru.chousik.kt_blps.model.ChatMessageOutboxStatus

interface ChatMessageOutboxRepository : JpaRepository<ChatMessageOutbox, UUID> {
    fun findTop50ByStatusAndAvailableAtLessThanEqualOrderByCreatedAtAsc(
        status: ChatMessageOutboxStatus,
        availableAt: OffsetDateTime
    ): List<ChatMessageOutbox>
}

