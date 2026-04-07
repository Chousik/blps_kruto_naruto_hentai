package ru.chousik.blps_kt.api.chat

import java.time.OffsetDateTime
import java.util.UUID
import ru.chousik.blps_kt.model.ChatMessage
import ru.chousik.blps_kt.model.ChatMessageType

data class ChatMessageResponse(
    val id: UUID,
    val chatId: UUID,
    val type: ChatMessageType,
    val senderUserId: UUID?,
    val senderUsername: String?,
    val message: String,
    val createdAt: OffsetDateTime
) {
    companion object {
        fun from(entity: ChatMessage): ChatMessageResponse = ChatMessageResponse(
            id = entity.id,
            chatId = entity.chat.id,
            type = entity.type,
            senderUserId = entity.senderUser?.id,
            senderUsername = entity.senderUser?.username,
            message = entity.message,
            createdAt = entity.createdAt
        )
    }
}
