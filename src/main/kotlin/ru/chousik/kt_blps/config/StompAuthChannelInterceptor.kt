package ru.chousik.kt_blps.config

import java.nio.charset.StandardCharsets
import java.util.Base64
import java.util.UUID
import org.springframework.messaging.Message
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.MessageDeliveryException
import org.springframework.messaging.simp.stomp.StompCommand
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.messaging.support.ChannelInterceptor
import org.springframework.messaging.support.MessageHeaderAccessor
import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.stereotype.Component
import ru.chousik.kt_blps.security.XmlAccountPrincipal
import ru.chousik.kt_blps.service.ChatMessageService

@Component
class StompAuthChannelInterceptor(
    private val authenticationProvider: AuthenticationProvider,
    private val chatMessageService: ChatMessageService
) : ChannelInterceptor {

    private val chatMessagesTopic = Regex("^/topic/chats/([0-9a-fA-F-]{36})/messages$")

    override fun preSend(message: Message<*>, channel: MessageChannel): Message<*>? {
        val accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor::class.java) ?: return message
        when (accessor.command) {
            StompCommand.CONNECT -> authenticate(accessor)
            StompCommand.SUBSCRIBE -> authorizeSubscribe(accessor)
            else -> Unit
        }
        return message
    }

    private fun authenticate(accessor: StompHeaderAccessor) {
        val authHeader = accessor.getFirstNativeHeader("Authorization")
            ?: accessor.getFirstNativeHeader("authorization")
            ?: throw BadCredentialsException("missing Authorization header")

        if (!authHeader.startsWith("Basic ", ignoreCase = true)) {
            throw BadCredentialsException("Authorization header must use Basic scheme")
        }

        val token = authHeader.substringAfter("Basic ", "")
        if (token.isBlank()) {
            throw BadCredentialsException("empty basic token")
        }

        val decoded = try {
            String(Base64.getDecoder().decode(token), StandardCharsets.UTF_8)
        } catch (ex: IllegalArgumentException) {
            throw BadCredentialsException("invalid basic token", ex)
        }

        val separatorIndex = decoded.indexOf(':')
        if (separatorIndex <= 0) {
            throw BadCredentialsException("invalid basic credentials")
        }

        val username = decoded.take(separatorIndex)
        val password = decoded.substring(separatorIndex + 1)
        val authRequest = UsernamePasswordAuthenticationToken.unauthenticated(username, password)
        val authenticated = authenticationProvider.authenticate(authRequest)
            ?: throw BadCredentialsException("bad credentials")

        accessor.user = authenticated
    }

    private fun authorizeSubscribe(accessor: StompHeaderAccessor) {
        val destination = accessor.destination ?: return
        val match = chatMessagesTopic.matchEntire(destination) ?: return
        val chatId = UUID.fromString(match.groupValues[1])
        val principal = extractPrincipal(accessor)
        chatMessageService.requireReadAccess(chatId, principal.userId)
    }

    private fun extractPrincipal(accessor: StompHeaderAccessor): XmlAccountPrincipal {
        val authentication = accessor.user
            ?: throw MessageDeliveryException("unauthenticated")
        val principal = (authentication as? UsernamePasswordAuthenticationToken)?.principal
            ?: throw MessageDeliveryException("unauthenticated")
        return principal as? XmlAccountPrincipal
            ?: throw MessageDeliveryException("unauthenticated")
    }
}
