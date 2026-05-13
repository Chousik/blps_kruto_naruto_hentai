package ru.chousik.payment_worker_service.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(
    name = "payment_creation_sagas",
    uniqueConstraints = [UniqueConstraint(columnNames = ["extra_service_request_id"])]
)
class PaymentCreationSaga {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    lateinit var id: UUID

    @Column(name = "extra_service_request_id", nullable = false)
    lateinit var extraServiceRequestId: UUID

    @Column(name = "chat_id", nullable = false)
    lateinit var chatId: UUID

    @Column(name = "initiated_by_user_id", nullable = false)
    lateinit var initiatedByUserId: UUID

    @Column(name = "title", nullable = false, length = 255)
    lateinit var title: String

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    lateinit var amount: BigDecimal

    @Column(name = "currency", nullable = false, length = 3)
    lateinit var currency: String

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    lateinit var status: PaymentCreationSagaStatus

    @Column(name = "payment_request_id", unique = true)
    var paymentRequestId: UUID? = null

    @Column(name = "provider_payment_id", unique = true, length = 128)
    var providerPaymentId: String? = null

    @Column(name = "payment_url", length = 2000)
    var paymentUrl: String? = null

    @Column(name = "error_message", length = 4000)
    var errorMessage: String? = null

    @Column(name = "attempt_count", nullable = false)
    var attemptCount: Int = 0

    @Column(name = "attempted_at")
    var attemptedAt: OffsetDateTime? = null

    @Column(name = "created_at", nullable = false)
    lateinit var createdAt: OffsetDateTime

    @Column(name = "updated_at", nullable = false)
    lateinit var updatedAt: OffsetDateTime
}
