package ru.chousik.kt_blps.api.chat

import java.time.OffsetDateTime
import java.util.UUID
import ru.chousik.kt_blps.model.Chat

data class ChatResponse(
    val id: UUID,
    val guest: ChatUserDto,
    val host: ChatUserDto,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime
) {
    companion object {
        fun from(chat: Chat): ChatResponse = ChatResponse(
            id = chat.id,
            guest = ChatUserDto.from(chat.guest),
            host = ChatUserDto.from(chat.host),
            createdAt = chat.createdAt,
            updatedAt = chat.updatedAt
        )
    }
}
