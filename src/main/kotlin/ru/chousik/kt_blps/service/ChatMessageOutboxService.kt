package ru.chousik.blps_kt.service

import com.fasterxml.jackson.databind.json.JsonMapper
import java.time.OffsetDateTime
import java.util.UUID
import org.springframework.stereotype.Service
import ru.chousik.blps_kt.api.chat.ChatMessageResponse
import ru.chousik.blps_kt.model.ChatMessage
import ru.chousik.blps_kt.model.ChatMessageOutbox
import ru.chousik.blps_kt.model.ChatMessageOutboxStatus
import ru.chousik.blps_kt.repository.ChatMessageOutboxRepository

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
