package ru.chousik.kt_blps.repository

import java.time.OffsetDateTime
import java.util.UUID
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import ru.chousik.kt_blps.model.ErpSyncOutboxTask
import ru.chousik.kt_blps.model.ErpSyncOutboxTaskStatus
import ru.chousik.kt_blps.model.ErpSyncOutboxTaskType

interface ErpSyncOutboxTaskRepository : JpaRepository<ErpSyncOutboxTask, UUID> {
    fun findByTaskTypeAndAggregateId(taskType: ErpSyncOutboxTaskType, aggregateId: UUID): ErpSyncOutboxTask?

    fun findAllByStatusAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
        status: ErpSyncOutboxTaskStatus,
        nextAttemptAt: OffsetDateTime,
        pageable: Pageable
    ): List<ErpSyncOutboxTask>
}
