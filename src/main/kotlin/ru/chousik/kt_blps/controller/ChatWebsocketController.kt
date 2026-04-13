package ru.chousik.kt_blps.controller

import jakarta.validation.Valid
import java.util.UUID
import org.springframework.messaging.handler.annotation.DestinationVariable
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.messaging.MessageDeliveryException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.stereotype.Controller
import org.springframework.validation.annotation.Validated
import ru.chousik.kt_blps.dto.chat.CreateChatMessageRequest
import ru.chousik.kt_blps.security.XmlAccountPrincipal
import ru.chousik.kt_blps.service.ChatMessageService

@Controller
@Validated
class ChatWebsocketController(
    private val chatMessageService: ChatMessageService,
    private val messagingTemplate: SimpMessagingTemplate
) {

    @MessageMapping("/chats/{chatId}/messages")
    fun sendMessage(
        @DestinationVariable chatId: UUID,
        @Valid request: CreateChatMessageRequest,
        principal: java.security.Principal
    ) {
        val account = extractPrincipal(principal)
        val response = chatMessageService.createMessage(chatId, account.userId, request)
        messagingTemplate.convertAndSend("/topic/chats/$chatId/messages", response)
    }

    private fun extractPrincipal(principal: java.security.Principal): XmlAccountPrincipal {
        val authentication = principal as? UsernamePasswordAuthenticationToken
            ?: throw MessageDeliveryException("unauthenticated")
        val account = authentication.principal as? XmlAccountPrincipal
            ?: throw MessageDeliveryException("unauthenticated")
        return account
    }
}
