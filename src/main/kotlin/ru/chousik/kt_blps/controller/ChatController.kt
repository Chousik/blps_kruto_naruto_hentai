package ru.chousik.kt_blps.controller

import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import java.util.UUID
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.access.prepost.PreAuthorize
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
import ru.chousik.kt_blps.security.XmlAccountPrincipal
import ru.chousik.kt_blps.service.ChatService

@RestController
@Validated
@RequestMapping
class ChatController(
    private val chatService: ChatService
) {

    @PreAuthorize("hasAuthority('PRIV_CHAT_CREATE')")
    @PostMapping("/chats")
    fun createChat(
        @AuthenticationPrincipal(errorOnInvalidType = true) authenticatedAccount: XmlAccountPrincipal,
        @Valid @RequestBody request: CreateChatRequest
    ): ChatResponse {
        val chat = chatService.createChat(authenticatedAccount.userId, request)
        return ChatResponse.from(chat)
    }

    @PreAuthorize("hasAuthority('PRIV_CHAT_READ')")
    @GetMapping("/chats/{chatId}")
    fun getChat(
        @PathVariable chatId: UUID,
        @AuthenticationPrincipal(errorOnInvalidType = true) authenticatedAccount: XmlAccountPrincipal
    ): ChatResponse {
        val chat = chatService.getChatForUser(chatId, authenticatedAccount.userId)
        return ChatResponse.from(chat)
    }

    @PreAuthorize("hasAuthority('PRIV_CHAT_LIST')")
    @GetMapping("/chats")
    fun getUserChats(
        @AuthenticationPrincipal(errorOnInvalidType = true) authenticatedAccount: XmlAccountPrincipal,
        @RequestParam("userId", required = false) userId: UUID?,
        @RequestParam("limit", defaultValue = "20") @Min(1) @Max(100) limit: Int,
        @RequestParam("offset", defaultValue = "0") @Min(0) offset: Long
    ): PagedChatsResponse {
        val page = chatService.getChatsForUser(authenticatedAccount.userId, userId, limit, offset)
        return PagedChatsResponse(
            items = page.content.map { ChatResponse.from(it) },
            total = page.totalElements,
            limit = limit,
            offset = offset
        )
    }
}
