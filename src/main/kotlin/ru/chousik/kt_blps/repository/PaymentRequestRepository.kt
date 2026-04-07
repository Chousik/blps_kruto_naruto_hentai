package ru.chousik.kt_blps.repository

import java.util.UUID
import org.springframework.data.jpa.repository.JpaRepository
import ru.chousik.kt_blps.model.PaymentRequest

interface PaymentRequestRepository : JpaRepository<PaymentRequest, UUID> {
    fun findAllByExtraServiceRequestIdOrderByCreatedAtDesc(extraServiceRequestId: UUID): List<PaymentRequest>

    fun findFirstByExtraServiceRequestIdOrderByCreatedAtDesc(extraServiceRequestId: UUID): PaymentRequest?

    fun findByProviderPaymentId(providerPaymentId: String): PaymentRequest?
}
