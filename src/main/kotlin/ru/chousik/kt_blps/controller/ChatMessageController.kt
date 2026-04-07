package ru.chousik.kt_blps.controller

import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import java.util.UUID
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import ru.chousik.kt_blps.dto.chat.ChatMessageResponse
import ru.chousik.kt_blps.dto.chat.CreateChatMessageRequest
import ru.chousik.kt_blps.dto.chat.PagedChatMessagesResponse
import ru.chousik.kt_blps.service.ChatMessageService

@RestController
@Validated
@RequestMapping
class ChatMessageController(
    private val chatMessageService: ChatMessageService
) {

    @PostMapping("/chats/{chatId}/messages")
    fun createMessage(
        @PathVariable chatId: UUID,
        @Valid @RequestBody request: CreateChatMessageRequest
    ): ChatMessageResponse =
        chatMessageService.createMessage(chatId, requesterId, request)

    @GetMapping("/chats/{chatId}/messages")
    fun getMessages(
        @PathVariable chatId: UUID,
        @RequestParam("limit", defaultValue = "20") @Min(1) @Max(100) limit: Int,
        @RequestParam("offset", defaultValue = "0") @Min(0) offset: Long
    ): PagedChatMessagesResponse {
        val page = chatMessageService.getMessages(chatId, requesterId, limit, offset)
        return PagedChatMessagesResponse(
            items = page.content,
            total = page.totalElements,
            limit = limit,
            offset = offset
        )
    }

    @GetMapping("/chats/{chatId}/messages/{messageId}")
    fun getMessage(
        @PathVariable chatId: UUID,
        @PathVariable messageId: UUID,
        @RequestParam("requesterId") requesterId: UUID
    ): ChatMessageResponse =
        chatMessageService.getMessage(chatId, messageId, requesterId)
}
