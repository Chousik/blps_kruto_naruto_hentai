package ru.chousik.blps_kt.service

import java.time.OffsetDateTime
import java.util.UUID
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionTemplate
import org.springframework.web.server.ResponseStatusException
import ru.chousik.blps_kt.api.chat.ChatMessageResponse
import ru.chousik.blps_kt.api.chat.CreateChatMessageRequest
import ru.chousik.blps_kt.model.Chat
import ru.chousik.blps_kt.model.ChatMessage
import ru.chousik.blps_kt.model.User
import ru.chousik.blps_kt.model.UserRole
import ru.chousik.blps_kt.pagination.OffsetBasedPageRequest
import ru.chousik.blps_kt.repository.ChatMessageRepository
import ru.chousik.blps_kt.repository.ChatRepository
import ru.chousik.blps_kt.repository.UserRepository
import ru.chousik.blps_kt.security.CurrentAccountService

@Service
class ChatMessageService(
    private val chatRepository: ChatRepository,
    private val userRepository: UserRepository,
    private val chatMessageRepository: ChatMessageRepository,
    private val chatMessageOutboxService: ChatMessageOutboxService,
    private val currentAccountService: CurrentAccountService,
    @Qualifier("jtaTransactionTemplate")
    private val transactionTemplate: TransactionTemplate
) {

    fun createMessage(
        chatId: UUID,
        request: CreateChatMessageRequest
    ): ChatMessageResponse {
        val requesterId = currentAccountService.currentAccount().userId
        return createMessage(chatId, requesterId, request)
    }

    fun createMessage(
        chatId: UUID,
        requesterId: UUID,
        request: CreateChatMessageRequest
    ): ChatMessageResponse =
        requireNotNull(
            transactionTemplate.execute {
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
                chatMessageOutboxService.enqueue(savedMessage)
                ChatMessageResponse.from(savedMessage)
            }
        ) { "chat message creation transaction returned null result" }
    

    @Transactional(readOnly = true)
    fun getMessages(
        chatId: UUID,
        limit: Int,
        offset: Long
    ): org.springframework.data.domain.Page<ChatMessageResponse> {
        val requesterId = currentAccountService.currentAccount().userId
        return getMessages(chatId, requesterId, limit, offset)
    }

    @Transactional(readOnly = true)
    fun getMessages(
        chatId: UUID,
        requesterId: UUID,
        limit: Int,
        offset: Long
    ): org.springframework.data.domain.Page<ChatMessageResponse> {
        requireReadAccess(chatId, requesterId)

        val pageable = OffsetBasedPageRequest(limit, offset, Sort.by(Sort.Direction.ASC, "createdAt"))
        return chatMessageRepository.findAllByChatId(chatId, pageable)
            .map { ChatMessageResponse.from(it) }
    }

    @Transactional(readOnly = true)
    fun getMessage(
        chatId: UUID,
        messageId: UUID
    ): ChatMessageResponse {
        val requesterId = currentAccountService.currentAccount().userId
        return getMessage(chatId, messageId, requesterId)
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
