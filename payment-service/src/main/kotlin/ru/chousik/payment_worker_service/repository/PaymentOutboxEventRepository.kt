package ru.chousik.payment_worker_service.repository

import jakarta.persistence.LockModeType
import java.time.OffsetDateTime
import java.util.UUID
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import ru.chousik.payment_worker_service.model.PaymentOutboxEvent
import ru.chousik.payment_worker_service.model.PaymentOutboxEventStatus

interface PaymentOutboxEventRepository : JpaRepository<PaymentOutboxEvent, UUID> {
    fun existsByPayload(payload: String): Boolean

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from PaymentOutboxEvent e where e.id = :id")
    fun findByIdForUpdate(@Param("id") id: UUID): PaymentOutboxEvent?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
        """
        select e
        from PaymentOutboxEvent e
        where e.status in :statuses
          and e.attemptedAt <= :attemptedBefore
          and e.attemptCount < :maxAttempts
        order by e.attemptedAt asc, e.createdAt asc
        """
    )
    fun findReadyBatch(
        @Param("statuses") statuses: Collection<PaymentOutboxEventStatus>,
        @Param("attemptedBefore") attemptedBefore: OffsetDateTime,
        @Param("maxAttempts") maxAttempts: Int,
        pageable: Pageable
    ): List<PaymentOutboxEvent>
}
