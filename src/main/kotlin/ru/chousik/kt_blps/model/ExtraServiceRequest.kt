package ru.chousik.blps_kt.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
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
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID
import org.hibernate.annotations.Fetch
import org.hibernate.annotations.FetchMode

@Entity
@Table(name = "extra_service_requests")
class ExtraServiceRequest {

    @Id
    lateinit var id: UUID

    @field:NotNull
    @ManyToOne(optional = false)
    @Fetch(FetchMode.JOIN)
    @JoinColumn(name = "chat_id", nullable = false)
    lateinit var chat: Chat

    @field:NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    lateinit var status: ExtraServiceRequestStatus

    @field:NotBlank
    @field:Size(max = 255)
    @Column(name = "title", nullable = false, length = 255)
    lateinit var title: String

    @field:NotBlank
    @field:Size(max = 2000)
    @Column(name = "description", nullable = false, length = 2000)
    lateinit var description: String

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
    @field:PastOrPresent
    @Column(name = "created_at", nullable = false)
    lateinit var createdAt: OffsetDateTime

    @field:NotNull
    @field:PastOrPresent
    @Column(name = "updated_at", nullable = false)
    lateinit var updatedAt: OffsetDateTime
}
