package ru.chousik.kt_blps.service

import java.time.OffsetDateTime
import java.util.UUID
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import ru.chousik.kt_blps.dto.chat.CreateChatMessageRequest
import ru.chousik.kt_blps.model.Chat
import ru.chousik.kt_blps.model.User
import ru.chousik.kt_blps.model.UserRole
import ru.chousik.kt_blps.repository.ChatMessageRepository
import ru.chousik.kt_blps.repository.ChatRepository
import ru.chousik.kt_blps.repository.UserRepository

@SpringBootTest
class ChatMessageServiceIntegrationTest {

    @Autowired
    private lateinit var chatMessageService: ChatMessageService

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var chatRepository: ChatRepository

    @Autowired
    private lateinit var chatMessageRepository: ChatMessageRepository

    private val chatIdsToDelete = mutableListOf<UUID>()
    private val userIdsToDelete = mutableListOf<UUID>()
    private val messageIdsToDelete = mutableListOf<UUID>()

    @AfterEach
    fun cleanup() {
        if (messageIdsToDelete.isNotEmpty()) {
            chatMessageRepository.deleteAllById(messageIdsToDelete)
        }
        if (chatIdsToDelete.isNotEmpty()) {
            chatRepository.deleteAllById(chatIdsToDelete)
        }
        if (userIdsToDelete.isNotEmpty()) {
            userRepository.deleteAllById(userIdsToDelete)
        }
    }

    @Test
    fun `createMessage generates UUID and persists chat message`() {
        val guest = userRepository.save(newUser(UserRole.GUEST, "guest"))
        val host = userRepository.save(newUser(UserRole.HOST, "host"))
        userIdsToDelete += guest.id
        userIdsToDelete += host.id

        val now = OffsetDateTime.now()
        val chat = chatRepository.save(Chat().apply {
            this.guest = guest
            this.host = host
            createdAt = now
            updatedAt = now
        })
        chatIdsToDelete += chat.id

        val response = chatMessageService.createMessage(
            chatId = chat.id,
            requesterId = guest.id,
            request = CreateChatMessageRequest(message = "test message")
        )
        messageIdsToDelete += response.id

        val savedMessage = chatMessageRepository.findById(response.id).orElseThrow()
        assertThat(response.id).isNotNull
        assertThat(savedMessage.id).isEqualTo(response.id)
        assertThat(savedMessage.chat.id).isEqualTo(chat.id)
        assertThat(savedMessage.senderUser?.id).isEqualTo(guest.id)
        assertThat(savedMessage.message).isEqualTo("test message")
    }

    private fun newUser(role: UserRole, prefix: String): User {
        val token = UUID.randomUUID().toString().replace("-", "")
        val now = OffsetDateTime.now()
        return User().apply {
            username = "${prefix}_$token"
            email = "$token@example.com"
            firstName = prefix.replaceFirstChar { it.uppercase() }
            lastName = "Test"
            this.role = role
            createdAt = now
            updatedAt = now
        }
    }
}
