package ru.chousik.kt_blps.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.PastOrPresent
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(
    name = "payment_request_reminders",
    uniqueConstraints = [UniqueConstraint(columnNames = ["payment_request_id", "type"])]
)
class PaymentRequestReminder {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    lateinit var id: UUID

    @field:NotNull
    @Column(name = "payment_request_id", nullable = false)
    lateinit var paymentRequestId: UUID

    @field:NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 32)
    lateinit var type: PaymentLinkReminderType

    @field:NotNull
    @field:PastOrPresent
    @Column(name = "sent_at", nullable = false)
    lateinit var sentAt: OffsetDateTime
}
