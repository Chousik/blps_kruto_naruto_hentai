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
import ru.chousik.kt_blps.dto.chat.CreateChatRequest
import ru.chousik.kt_blps.model.Chat
import ru.chousik.kt_blps.model.User
import ru.chousik.kt_blps.model.UserRole
import ru.chousik.kt_blps.pagination.OffsetBasedPageRequest
import ru.chousik.kt_blps.repository.ChatRepository
import ru.chousik.kt_blps.repository.UserRepository

@Service
class ChatService(
    private val chatRepository: ChatRepository,
    private val userRepository: UserRepository,
    private val chatSystemMessageService: ChatSystemMessageService,
    @Qualifier("writeTransactionTemplate")
    private val writeTransactionTemplate: TransactionTemplate,
    @Qualifier("readOnlyTransactionTemplate")
    private val readOnlyTransactionTemplate: TransactionTemplate
) {

    fun createChat(requesterId: UUID, request: CreateChatRequest): Chat =
        writeTransactionTemplate.execute {
            val guest = loadUser(request.guestUserId!!)
            val host = loadUser(request.hostUserId!!)
            val initiator = loadUser(requesterId)

            if (guest.id == host.id) {
                throw ResponseStatusException(HttpStatus.BAD_REQUEST, "guest and host must be different users")
            }

            requireRole(guest, UserRole.GUEST, "guest must have GUEST role")
            requireRole(host, UserRole.HOST, "host must have HOST role")

            if (!canInitiateChat(initiator, guest, host)) {
                throw ResponseStatusException(HttpStatus.FORBIDDEN, "initiator must be guest, host, or admin user")
            }

            if (chatRepository.existsByGuestIdAndHostId(guest.id, host.id)) {
                throw ResponseStatusException(HttpStatus.CONFLICT, "chat between guest and host already exists")
            }

            val now = OffsetDateTime.now()
            val chat = Chat().apply {
                this.guest = guest
                this.host = host
                createdAt = now
                updatedAt = now
            }
            val savedChat = chatRepository.save(chat)
            chatSystemMessageService.append(
                chat = savedChat,
                message = "Chat has been created for guest ${guest.username} and host ${host.username}."
            )
            savedChat
        }

    fun getChatForUser(chatId: UUID, requesterId: UUID): Chat =
        readOnlyTransactionTemplate.execute<Chat> {
            val chat = chatRepository.findById(chatId)
                .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "chat not found") }
            val requester = loadUser(requesterId)

            if (!canAccessChat(chat, requester)) {
                throw ResponseStatusException(HttpStatus.FORBIDDEN, "user cannot access this chat")
            }
            chat
        }

    fun getChatsForUser(requesterId: UUID, userId: UUID?, limit: Int, offset: Long): Page<Chat> =
        readOnlyTransactionTemplate.execute {
            val requester = loadUser(requesterId)
            val targetUser = loadUser(userId ?: requesterId)

            if (requester.role != UserRole.ADMIN && requester.id != targetUser.id) {
                throw ResponseStatusException(HttpStatus.FORBIDDEN, "only the user or admin can view chats")
            }

            val pageable = OffsetBasedPageRequest(limit, offset, Sort.by(Sort.Direction.DESC, "createdAt"))
            chatRepository.findAllByGuestIdOrHostId(targetUser.id, targetUser.id, pageable)
        }

    private fun loadUser(userId: UUID): User =
        userRepository.findById(userId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "user not found") }

    private fun requireRole(user: User, expectedRole: UserRole, message: String) {
        if (user.role != expectedRole) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, message)
        }
    }

    private fun canInitiateChat(initiator: User, guest: User, host: User): Boolean =
        when (initiator.role) {
            UserRole.GUEST -> initiator.id == guest.id
            UserRole.HOST -> initiator.id == host.id
            UserRole.ADMIN -> true
        }

    private fun canAccessChat(chat: Chat, requester: User): Boolean =
        when (requester.role) {
            UserRole.GUEST -> requester.id == chat.guest.id
            UserRole.HOST -> requester.id == chat.host.id
            UserRole.ADMIN -> true
        }
}
