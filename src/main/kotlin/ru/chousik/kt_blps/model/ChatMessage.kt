package ru.chousik.blps_kt.model

import jakarta.persistence.*
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.PastOrPresent
import jakarta.validation.constraints.Size
import java.time.OffsetDateTime
import java.util.UUID
import org.hibernate.annotations.Fetch
import org.hibernate.annotations.FetchMode

@Entity
@Table(name = "chat_messages")
class ChatMessage {

    @Id
    lateinit var id: UUID

    @field:NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "chat_id", nullable = false)
    lateinit var chat: Chat

    @ManyToOne
    @Fetch(FetchMode.JOIN)
    @JoinColumn(name = "sender_user_id")
    var senderUser: User? = null

    @field:NotBlank
    @field:Size(max = 2000)
    @Column(name = "message", nullable = false, length = 2000)
    lateinit var message: String

    @field:NotNull
    @field:PastOrPresent
    @Column(name = "created_at", nullable = false)
    lateinit var createdAt: OffsetDateTime

    @get:Transient
    val type: ChatMessageType
        get() = if (senderUser == null) ChatMessageType.PLATFORM else ChatMessageType.USER
}
