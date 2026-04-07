package ru.chousik.blps_kt.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.validation.constraints.Digits
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.PastOrPresent
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import org.hibernate.annotations.Fetch
import org.hibernate.annotations.FetchMode
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(name = "receipts")
class Receipt {

    @Id
    lateinit var id: UUID

    @field:NotNull
    @Column(name = "payment_request_id", nullable = false, unique = true)
    lateinit var paymentRequestId: UUID

    @field:NotBlank
    @field:Size(max = 64)
    @Column(name = "receipt_number", nullable = false, unique = true, length = 64)
    lateinit var receiptNumber: String

    @field:NotNull
    @field:Positive
    @field:Digits(integer = 10, fraction = 2)
    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    lateinit var amount: BigDecimal

    @field:NotBlank
    @field:Pattern(regexp = "^[A-Z]{3}$")
    @Column(name = "currency", nullable = false, length = 3)
    lateinit var currency: String

    @field:NotNull
    @ManyToOne(optional = false)
    @Fetch(FetchMode.JOIN)
    @JoinColumn(name = "issued_by_user_id", nullable = false)
    lateinit var issuedBy: User

    @field:NotNull
    @field:PastOrPresent
    @Column(name = "issued_at", nullable = false)
    lateinit var issuedAt: OffsetDateTime
}
