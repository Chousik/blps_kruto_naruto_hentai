package ru.chousik.blps_kt.controller

import jakarta.validation.Valid
import java.security.Principal
import java.util.UUID
import org.springframework.messaging.handler.annotation.DestinationVariable
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Controller
import org.springframework.validation.annotation.Validated
import ru.chousik.blps_kt.api.chat.CreateChatMessageRequest
import ru.chousik.blps_kt.security.CurrentAccountService
import ru.chousik.blps_kt.service.ChatMessageService

@Controller
@Validated
class ChatMessageWsController(
    private val chatMessageService: ChatMessageService,
    private val currentAccountService: CurrentAccountService
) {

    @MessageMapping("/chats/{chatId}/messages")
    fun sendMessage(
        @DestinationVariable chatId: UUID,
        @Valid @Payload request: CreateChatMessageRequest,
        principal: Principal
    ) {
        val currentAccount = currentAccountService.fromPrincipal(principal)
        chatMessageService.createMessage(chatId, currentAccount.userId, request)
    }
}
