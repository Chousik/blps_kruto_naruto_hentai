package ru.chousik.kt_blps.service

import com.fasterxml.jackson.databind.json.JsonMapper
import java.time.OffsetDateTime
import java.util.UUID
import org.springframework.stereotype.Service
import ru.chousik.kt_blps.api.chat.ChatMessageResponse
import ru.chousik.kt_blps.model.ChatMessage
import ru.chousik.kt_blps.model.ChatMessageOutbox
import ru.chousik.kt_blps.model.ChatMessageOutboxStatus
import ru.chousik.kt_blps.repository.ChatMessageOutboxRepository

@Service
class ChatMessageOutboxService(
    private val chatMessageOutboxRepository: ChatMessageOutboxRepository
) {

    private val objectMapper = JsonMapper.builder()
        .findAndAddModules()
        .build()

    fun enqueue(chatMessage: ChatMessage) {
        val payload = ChatMessageResponse.from(chatMessage)
        val now = OffsetDateTime.now()

        val record = ChatMessageOutbox().apply {
            id = UUID.randomUUID()
            chatMessageId = chatMessage.id
            destination = "/topic/chats/${chatMessage.chat.id}"
            this.payload = objectMapper.writeValueAsString(payload)
            status = ChatMessageOutboxStatus.PENDING
            attempts = 0
            availableAt = now
            createdAt = now
        }

        chatMessageOutboxRepository.save(record)
    }
}
