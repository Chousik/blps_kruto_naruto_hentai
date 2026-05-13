package ru.chousik.payment_worker_service.model

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
@Table(name = "payment_worker_outbox_events")
class PaymentOutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    lateinit var id: UUID

    @Lob
    @Column(name = "payload", nullable = false, columnDefinition = "text")
    lateinit var payload: String

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    lateinit var status: PaymentOutboxEventStatus

    @Column(name = "attempt_count", nullable = false)
    var attemptCount: Int = 0

    @Column(name = "attempted_at", nullable = false)
    lateinit var attemptedAt: OffsetDateTime

    @Column(name = "last_error", length = 2000)
    var lastError: String? = null

    @Column(name = "created_at", nullable = false)
    lateinit var createdAt: OffsetDateTime
}
