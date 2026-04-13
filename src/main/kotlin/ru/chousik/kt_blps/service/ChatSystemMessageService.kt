package ru.chousik.kt_blps.service

import java.time.OffsetDateTime
import java.util.UUID
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import ru.chousik.kt_blps.dto.chat.ChatMessageResponse
import ru.chousik.kt_blps.model.Chat
import ru.chousik.kt_blps.model.ChatMessage
import ru.chousik.kt_blps.repository.ChatMessageRepository

@Service
class ChatSystemMessageService(
    private val chatMessageRepository: ChatMessageRepository,
    private val messagingTemplate: SimpMessagingTemplate
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
        val response = ChatMessageResponse.from(saved)
        val destination = "/topic/chats/${chat.id}/messages"
        sendAfterCommit { messagingTemplate.convertAndSend(destination, response) }
    }

    private fun sendAfterCommit(action: () -> Unit) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(object : TransactionSynchronization {
                override fun afterCommit() {
                    action()
                }
            })
        } else {
            action()
        }
    }
}
