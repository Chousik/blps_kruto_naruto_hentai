package ru.chousik.blps_kt.repository

import java.util.UUID
import org.springframework.data.jpa.repository.JpaRepository
import ru.chousik.blps_kt.model.PaymentRequest

interface PaymentRequestRepository : JpaRepository<PaymentRequest, UUID> {
    fun findAllByExtraServiceRequestIdOrderByCreatedAtDesc(extraServiceRequestId: UUID): List<PaymentRequest>

    fun findFirstByExtraServiceRequestIdOrderByCreatedAtDesc(extraServiceRequestId: UUID): PaymentRequest?

    fun findByProviderPaymentId(providerPaymentId: String): PaymentRequest?
}
