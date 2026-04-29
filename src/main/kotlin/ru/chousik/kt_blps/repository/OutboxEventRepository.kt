package ru.chousik.kt_blps.repository

import java.time.OffsetDateTime
import java.util.UUID
import jakarta.persistence.LockModeType
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import ru.chousik.kt_blps.model.OutboxEvent
import ru.chousik.kt_blps.model.OutboxEventStatus

interface OutboxEventRepository : JpaRepository<OutboxEvent, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
        """
        select e
        from OutboxEvent e
        where e.status = :pendingStatus
           or (e.status = :inProgressStatus and e.lockedAt is not null and e.lockedAt < :staleLockThreshold)
        order by e.createdAt asc
        """
    )
    fun findBatchForProcessing(
        @Param("pendingStatus") pendingStatus: OutboxEventStatus,
        @Param("inProgressStatus") inProgressStatus: OutboxEventStatus,
        @Param("staleLockThreshold") staleLockThreshold: OffsetDateTime,
        pageable: Pageable
    ): List<OutboxEvent>
}
