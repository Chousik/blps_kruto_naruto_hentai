package ru.chousik.blps_kt.service

import java.time.OffsetDateTime
import java.util.UUID
import org.springframework.stereotype.Service
import ru.chousik.blps_kt.model.Chat
import ru.chousik.blps_kt.model.ChatMessage
import ru.chousik.blps_kt.repository.ChatMessageRepository

@Service
class ChatSystemMessageService(
    private val chatMessageRepository: ChatMessageRepository,
    private val chatMessageOutboxService: ChatMessageOutboxService
) {

    fun append(chat: Chat, message: String) {
        val entity = ChatMessage().apply {
            id = UUID.randomUUID()
            this.chat = chat
            senderUser = null
            this.message = message
            createdAt = OffsetDateTime.now()
        }
        val saved = chatMessageRepository.save(entity)
        chatMessageOutboxService.enqueue(saved)
    }
}
