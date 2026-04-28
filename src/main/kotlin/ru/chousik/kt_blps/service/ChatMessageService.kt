package ru.chousik.kt_blps.service

import java.time.OffsetDateTime
import java.util.UUID
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.domain.Page
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate
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
    private val chatMessageRepository: ChatMessageRepository,
    @Qualifier("writeTransactionTemplate")
    private val writeTransactionTemplate: TransactionTemplate,
    @Qualifier("readOnlyTransactionTemplate")
    private val readOnlyTransactionTemplate: TransactionTemplate
) {

    fun createMessage(
        chatId: UUID,
        requesterId: UUID,
        request: CreateChatMessageRequest
    ): ChatMessageResponse = writeTransactionTemplate.execute {
        val chat = requireWriteAccessWithinTransaction(chatId, requesterId)
        val requester = loadUser(requesterId)
        val now = OffsetDateTime.now()

        val message = ChatMessage().apply {
            this.chat = chat
            senderUser = requester
            this.message = request.message!!.trim()
            createdAt = now
        }

        chat.updatedAt = now
        chatRepository.save(chat)
        val savedMessage = chatMessageRepository.saveAndFlush(message)
        ChatMessageResponse.from(savedMessage)
    }

    fun getMessages(
        chatId: UUID,
        requesterId: UUID,
        limit: Int,
        offset: Long
    ): Page<ChatMessageResponse> = readOnlyTransactionTemplate.execute {
        requireReadAccessWithinTransaction(chatId, requesterId)

        val pageable = OffsetBasedPageRequest(limit, offset, Sort.by(Sort.Direction.ASC, "createdAt"))
        chatMessageRepository.findAllByChatId(chatId, pageable)
            .map { ChatMessageResponse.from(it) }
    }

    fun requireReadAccess(chatId: UUID, requesterId: UUID) {
        readOnlyTransactionTemplate.executeWithoutResult {
            requireReadAccessWithinTransaction(chatId, requesterId)
        }
    }

    private fun requireReadAccessWithinTransaction(chatId: UUID, requesterId: UUID) {
        val chat = loadChat(chatId)
        val requester = loadUser(requesterId)
        ensureParticipantOrAdminCanRead(chat, requester)
    }

    private fun requireWriteAccessWithinTransaction(chatId: UUID, requesterId: UUID): Chat {
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
