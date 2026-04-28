package ru.chousik.payment_worker_service.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(name = "payment_requests")
class PaymentRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    lateinit var id: UUID

    @Column(name = "extra_service_request_id", nullable = false, unique = true)
    lateinit var extraServiceRequestId: UUID

    @Column(name = "initiated_by_user_id", nullable = false)
    lateinit var initiatedByUserId: UUID

    @Column(name = "provider_payment_id", unique = true, length = 128)
    var providerPaymentId: String? = null

    @Column(name = "payment_url", length = 2000)
    var paymentUrl: String? = null

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    lateinit var status: PaymentRequestStatus

    @Column(name = "created_at", nullable = false)
    lateinit var createdAt: OffsetDateTime

    @Column(name = "expires_at")
    var expiresAt: OffsetDateTime? = null

    @Column(name = "resolved_at")
    var resolvedAt: OffsetDateTime? = null
}
