package ru.chousik.blps_kt.config

import java.util.UUID
import org.springframework.messaging.Message
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.MessageDeliveryException
import org.springframework.messaging.simp.stomp.StompCommand
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.messaging.support.ChannelInterceptor
import org.springframework.stereotype.Component
import ru.chousik.blps_kt.security.CurrentAccountService
import ru.chousik.blps_kt.security.Privilege
import ru.chousik.blps_kt.service.ChatMessageService

@Component
class WebSocketAuthChannelInterceptor(
    private val chatMessageService: ChatMessageService,
    private val currentAccountService: CurrentAccountService
) : ChannelInterceptor {

    override fun preSend(message: Message<*>, channel: MessageChannel): Message<*> {
        val accessor = StompHeaderAccessor.wrap(message)
        val destination = accessor.destination ?: return message

        when (accessor.command) {
            StompCommand.SEND -> {
                if (destination.startsWith("/app/chats/") && destination.endsWith("/messages")) {
                    val currentAccount = currentAccountService.fromPrincipal(accessor.user)
                    requireAuthority(currentAccount.hasAuthority(Privilege.PRIV_CHAT_MESSAGE_WRITE.name), "chat write access")
                    chatMessageService.requireWriteAccess(
                        extractChatId(destination, "/app/chats/", "/messages"),
                        currentAccount.userId
                    )
                }
            }

            StompCommand.SUBSCRIBE -> {
                if (destination.startsWith("/topic/chats/")) {
                    val currentAccount = currentAccountService.fromPrincipal(accessor.user)
                    requireAuthority(currentAccount.hasAuthority(Privilege.PRIV_CHAT_MESSAGE_READ.name), "chat read access")
                    chatMessageService.requireReadAccess(
                        extractChatId(destination, "/topic/chats/", ""),
                        currentAccount.userId
                    )
                }
            }

            else -> Unit
        }

        return message
    }

    private fun requireAuthority(condition: Boolean, authorityName: String) {
        if (!condition) {
            throw MessageDeliveryException("authenticated user does not have $authorityName")
        }
    }

    private fun extractChatId(destination: String, prefix: String, suffix: String): UUID {
        val value = destination.removePrefix(prefix).removeSuffix(suffix)
        return try {
            UUID.fromString(value)
        } catch (_: IllegalArgumentException) {
            throw MessageDeliveryException("chatId in destination must be a valid UUID")
        }
    }
}
