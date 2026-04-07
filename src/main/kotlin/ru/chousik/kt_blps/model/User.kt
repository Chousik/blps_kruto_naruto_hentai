package ru.chousik.blps_kt.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.PastOrPresent
import jakarta.validation.constraints.Size
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(name = "users")
class User {

    @Id
    lateinit var id: UUID

    @field:NotBlank
    @field:Size(max = 128)
    @Column(name = "username", nullable = false, unique = true, length = 128)
    lateinit var username: String

    @field:NotBlank
    @field:Email
    @field:Size(max = 255)
    @Column(name = "email", nullable = false, unique = true, length = 255)
    lateinit var email: String

    @field:NotBlank
    @field:Size(max = 128)
    @Column(name = "first_name", nullable = false, length = 128)
    lateinit var firstName: String

    @field:NotBlank
    @field:Size(max = 128)
    @Column(name = "last_name", nullable = false, length = 128)
    lateinit var lastName: String

    @field:NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 32)
    lateinit var role: UserRole

    @field:NotNull
    @Column(name = "created_at", nullable = false)
    lateinit var createdAt: OffsetDateTime

    @field:NotNull
    @Column(name = "updated_at", nullable = false)
    lateinit var updatedAt: OffsetDateTime
}
