package ru.chousik.payment_worker_service.repository

import java.time.OffsetDateTime
import java.util.UUID
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import ru.chousik.payment_worker_service.model.PaymentCreationSaga
import ru.chousik.payment_worker_service.model.PaymentCreationSagaStatus

interface PaymentCreationSagaRepository : JpaRepository<PaymentCreationSaga, UUID> {
    fun findByExtraServiceRequestId(extraServiceRequestId: UUID): PaymentCreationSaga?

    @Query(
        """
        select s
        from PaymentCreationSaga s
        where s.status in :statuses
          and s.attemptedAt is not null
          and s.attemptedAt <= :attemptedBefore
          and s.attemptCount < :maxAttempts
        order by s.attemptedAt asc, s.createdAt asc
        """
    )
    fun findRetryableSagas(
        @Param("statuses") statuses: Collection<PaymentCreationSagaStatus>,
        @Param("attemptedBefore") attemptedBefore: OffsetDateTime,
        @Param("maxAttempts") maxAttempts: Int,
        pageable: Pageable
    ): List<PaymentCreationSaga>
}
