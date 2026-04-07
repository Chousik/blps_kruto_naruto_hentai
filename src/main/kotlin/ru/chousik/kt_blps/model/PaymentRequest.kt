package ru.chousik.blps_kt.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.validation.constraints.FutureOrPresent
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.PastOrPresent
import jakarta.validation.constraints.Size
import java.time.OffsetDateTime
import java.util.UUID
import org.hibernate.validator.constraints.URL

@Entity
@Table(name = "payment_requests")
class PaymentRequest {

    @Id
    lateinit var id: UUID

    @field:NotNull
    @Column(name = "extra_service_request_id", nullable = false, unique = true)
    lateinit var extraServiceRequestId: UUID

    @field:NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "initiated_by_user_id", nullable = false)
    lateinit var initiatedBy: User

    @field:Size(max = 128)
    @Column(name = "provider_payment_id", unique = true, length = 128)
    var providerPaymentId: String? = null

    @field:Size(max = 2000)
    @field:URL
    @Column(name = "payment_url", length = 2000)
    var paymentUrl: String? = null

    @field:NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    lateinit var status: PaymentRequestStatus

    @field:NotNull
    @field:PastOrPresent
    @Column(name = "created_at", nullable = false)
    lateinit var createdAt: OffsetDateTime

    @field:FutureOrPresent
    @Column(name = "expires_at")
    var expiresAt: OffsetDateTime? = null

    @field:PastOrPresent
    @Column(name = "resolved_at")
    var resolvedAt: OffsetDateTime? = null
}
