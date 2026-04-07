package ru.chousik.kt_blps.service

import java.time.OffsetDateTime
import java.util.UUID
import org.springframework.data.domain.Page
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import ru.chousik.kt_blps.dto.chat.ChatMessageResponse
import ru.chousik.kt_blps.dto.chat.CreateChatMessageRequest
import ru.chousik.kt_blps.model.Chat
import ru.chousik.kt_blps.model.ChatMessage
import ru.chousik.kt_blps.model.User
import ru.chousik.kt_blps.model.UserRole
import ru.chousik.kt_blps.pagination.OffsetBasedPageRequest
import ru.chousik.kt_blps.repository.ChatMessageRepository
import ru.chousik.kt_blps.repository.ChatRepository
import ru.chousik.kt_blps.repository.UserRepository

@Service
class ChatMessageService(
    private val chatRepository: ChatRepository,
    private val userRepository: UserRepository,
    private val chatMessageRepository: ChatMessageRepository
) {

    @Transactional
    fun createMessage(
        chatId: UUID,
        requesterId: UUID,
        request: CreateChatMessageRequest
    ): ChatMessageResponse {
        val chat = requireWriteAccess(chatId, requesterId)
        val requester = loadUser(requesterId)
        val now = OffsetDateTime.now()

        val message = ChatMessage().apply {
            id = UUID.randomUUID()
            this.chat = chat
            senderUser = requester
            this.message = request.message!!.trim()
            createdAt = now
        }

        chat.updatedAt = now
        chatRepository.save(chat)
        val savedMessage = chatMessageRepository.save(message)
        return ChatMessageResponse.from(savedMessage)
    }

    @Transactional(readOnly = true)
    fun getMessages(
        chatId: UUID,
        requesterId: UUID,
        limit: Int,
        offset: Long
    ): Page<ChatMessageResponse> {
        requireReadAccess(chatId, requesterId)

        val pageable = OffsetBasedPageRequest(limit, offset, Sort.by(Sort.Direction.ASC, "createdAt"))
        return chatMessageRepository.findAllByChatId(chatId, pageable)
            .map { ChatMessageResponse.from(it) }
    }

    @Transactional(readOnly = true)
    fun getMessage(
        chatId: UUID,
        messageId: UUID,
        requesterId: UUID
    ): ChatMessageResponse {
        requireReadAccess(chatId, requesterId)

        val message = chatMessageRepository.findByIdAndChatId(messageId, chatId)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "chat message not found")

        return ChatMessageResponse.from(message)
    }

    @Transactional(readOnly = true)
    fun requireReadAccess(chatId: UUID, requesterId: UUID) {
        val chat = loadChat(chatId)
        val requester = loadUser(requesterId)
        ensureParticipantOrAdminCanRead(chat, requester)
    }

    @Transactional(readOnly = true)
    fun requireWriteAccess(chatId: UUID, requesterId: UUID): Chat {
        val chat = loadChat(chatId)
        val requester = loadUser(requesterId)
        ensureParticipantCanWrite(chat, requester)
        return chat
    }

    private fun loadChat(chatId: UUID): Chat =
        chatRepository.findById(chatId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "chat not found") }

    private fun loadUser(userId: UUID): User =
        userRepository.findById(userId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "user not found") }

    private fun ensureParticipantOrAdminCanRead(chat: Chat, requester: User) {
        val allowed = requester.role == UserRole.ADMIN ||
            requester.id == chat.guest.id ||
            requester.id == chat.host.id
        if (!allowed) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "user cannot access chat messages")
        }
    }

    private fun ensureParticipantCanWrite(chat: Chat, requester: User) {
        val allowed = requester.id == chat.guest.id || requester.id == chat.host.id
        if (!allowed) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "only chat participants can send messages")
        }
    }
}
