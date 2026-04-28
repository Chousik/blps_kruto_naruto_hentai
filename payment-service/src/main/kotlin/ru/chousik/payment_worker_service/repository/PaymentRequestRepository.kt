package ru.chousik.payment_worker_service.repository

import java.util.UUID
import org.springframework.data.jpa.repository.JpaRepository
import ru.chousik.payment_worker_service.model.PaymentRequest

interface PaymentRequestRepository : JpaRepository<PaymentRequest, UUID> {
    fun findByProviderPaymentId(providerPaymentId: String): PaymentRequest?
}
