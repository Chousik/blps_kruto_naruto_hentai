package ru.chousik.kt_blps.repository

import java.time.OffsetDateTime
import java.util.UUID
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.repository.query.Param
import org.springframework.data.jpa.repository.Query
import ru.chousik.kt_blps.model.PaymentRequest
import ru.chousik.kt_blps.model.PaymentRequestStatus

interface PaymentRequestRepository : JpaRepository<PaymentRequest, UUID> {
    fun findAllByExtraServiceRequestIdOrderByCreatedAtDesc(extraServiceRequestId: UUID): List<PaymentRequest>

    fun findFirstByExtraServiceRequestIdOrderByCreatedAtDesc(extraServiceRequestId: UUID): PaymentRequest?

    fun findByProviderPaymentId(providerPaymentId: String): PaymentRequest?

    @Query(
        """
        select p
        from PaymentRequest p
        where p.status = :status
          and p.paymentUrl is not null
          and p.expiresAt is not null
          and p.expiresAt > :fromTime
          and p.expiresAt <= :toTime
        order by p.expiresAt asc
        """
    )
    fun findExpiringPayments(
        @Param("status") status: PaymentRequestStatus,
        @Param("fromTime") fromTime: OffsetDateTime,
        @Param("toTime") toTime: OffsetDateTime
    ): List<PaymentRequest>
}
