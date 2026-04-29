package ru.chousik.kt_blps.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Lob
import jakarta.persistence.Table
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(name = "outbox_events")
class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    lateinit var id: UUID

    @Column(name = "aggregate_type", nullable = false, length = 100)
    lateinit var aggregateType: String

    @Column(name = "aggregate_id", nullable = false)
    lateinit var aggregateId: UUID

    @Column(name = "event_type", nullable = false, length = 200)
    lateinit var eventType: String

    @Column(name = "topic", nullable = false, length = 255)
    lateinit var topic: String

    @Column(name = "message_key", nullable = false, length = 255)
    lateinit var messageKey: String

    @Lob
    @Column(name = "payload", nullable = false, columnDefinition = "text")
    lateinit var payload: String

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    lateinit var status: OutboxEventStatus

    @Column(name = "attempt_count", nullable = false)
    var attemptCount: Int = 0

    @Column(name = "locked_at")
    var lockedAt: OffsetDateTime? = null

    @Column(name = "published_at")
    var publishedAt: OffsetDateTime? = null

    @Column(name = "last_error", length = 2000)
    var lastError: String? = null

    @Column(name = "created_at", nullable = false)
    lateinit var createdAt: OffsetDateTime

    @Column(name = "updated_at", nullable = false)
    lateinit var updatedAt: OffsetDateTime
}
