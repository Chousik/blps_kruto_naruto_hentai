package ru.chousik.kt_blps.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.PastOrPresent
import jakarta.validation.constraints.Size
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(
    name = "erp_sync_outbox_tasks",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_erp_sync_outbox_type_aggregate",
            columnNames = ["task_type", "aggregate_id"]
        )
    ]
)
class ErpSyncOutboxTask {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    lateinit var id: UUID

    @field:NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "task_type", nullable = false, length = 64)
    lateinit var taskType: ErpSyncOutboxTaskType

    @field:NotNull
    @Column(name = "aggregate_id", nullable = false)
    lateinit var aggregateId: UUID

    @field:NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    lateinit var status: ErpSyncOutboxTaskStatus

    @field:NotNull
    @Column(name = "attempt_count", nullable = false)
    var attemptCount: Int = 0

    @field:NotNull
    @Column(name = "next_attempt_at", nullable = false)
    lateinit var nextAttemptAt: OffsetDateTime

    @field:Size(max = 4000)
    @Column(name = "last_error", length = 4000)
    var lastError: String? = null

    @field:NotNull
    @field:PastOrPresent
    @Column(name = "created_at", nullable = false)
    lateinit var createdAt: OffsetDateTime

    @field:NotNull
    @field:PastOrPresent
    @Column(name = "updated_at", nullable = false)
    lateinit var updatedAt: OffsetDateTime
}
