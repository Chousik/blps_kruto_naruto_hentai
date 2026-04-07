package ru.chousik.blps_kt.repository

import java.util.UUID
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import ru.chousik.blps_kt.model.ChatMessage

interface ChatMessageRepository : JpaRepository<ChatMessage, UUID> {
    fun findAllByChatId(chatId: UUID, pageable: Pageable): Page<ChatMessage>

    fun findByIdAndChatId(id: UUID, chatId: UUID): ChatMessage?
}
