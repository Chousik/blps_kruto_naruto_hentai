package ru.chousik.blps_kt.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(name = "chat_message_outbox")
class ChatMessageOutbox {

    @Id
    lateinit var id: UUID

    @Column(name = "chat_message_id", nullable = false, unique = true)
    lateinit var chatMessageId: UUID

    @Column(name = "destination", nullable = false, length = 255)
    lateinit var destination: String

    @Column(name = "payload", nullable = false, columnDefinition = "text")
    lateinit var payload: String

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    lateinit var status: ChatMessageOutboxStatus

    @Column(name = "attempts", nullable = false)
    var attempts: Int = 0

    @Column(name = "available_at", nullable = false)
    lateinit var availableAt: OffsetDateTime

    @Column(name = "created_at", nullable = false)
    lateinit var createdAt: OffsetDateTime

    @Column(name = "last_attempt_at")
    var lastAttemptAt: OffsetDateTime? = null

    @Column(name = "sent_at")
    var sentAt: OffsetDateTime? = null

    @Column(name = "last_error", length = 1000)
    var lastError: String? = null
}

