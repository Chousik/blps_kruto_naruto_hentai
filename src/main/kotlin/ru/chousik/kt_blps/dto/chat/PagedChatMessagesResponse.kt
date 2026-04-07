package ru.chousik.kt_blps.dto.chat

data class PagedChatMessagesResponse(
    val items: List<ChatMessageResponse>,
    val total: Long,
    val limit: Int,
    val offset: Long
)
