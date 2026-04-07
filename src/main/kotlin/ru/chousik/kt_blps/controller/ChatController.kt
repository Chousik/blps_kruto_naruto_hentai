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
import ru.chousik.kt_blps.dto.chat.ChatResponse
import ru.chousik.kt_blps.dto.chat.CreateChatRequest
import ru.chousik.kt_blps.dto.chat.PagedChatsResponse
import ru.chousik.kt_blps.service.ChatService

@RestController
@Validated
@RequestMapping
class ChatController(
    private val chatService: ChatService
) {

    @PostMapping("/chats")
    fun createChat(
        @RequestParam("requesterId") requesterId: UUID,
        @Valid @RequestBody request: CreateChatRequest
    ): ChatResponse {
        val chat = chatService.createChat(requesterId, request)
        return ChatResponse.from(chat)
    }

    @GetMapping("/chats/{chatId}")
    fun getChat(
        @PathVariable chatId: UUID,
        @RequestParam("requesterId") requesterId: UUID
    ): ChatResponse {
        val chat = chatService.getChatForUser(chatId, requesterId)
        return ChatResponse.from(chat)
    }

    @GetMapping("/chats")
    fun getUserChats(
        @RequestParam("requesterId") requesterId: UUID,
        @RequestParam("userId", required = false) userId: UUID?,
        @RequestParam("limit", defaultValue = "20") @Min(1) @Max(100) limit: Int,
        @RequestParam("offset", defaultValue = "0") @Min(0) offset: Long
    ): PagedChatsResponse {
        val page = chatService.getChatsForUser(requesterId, userId, limit, offset)
        return PagedChatsResponse(
            items = page.content.map { ChatResponse.from(it) },
            total = page.totalElements,
            limit = limit,
            offset = offset
        )
    }
}
