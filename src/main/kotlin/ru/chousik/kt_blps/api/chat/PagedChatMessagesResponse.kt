package ru.chousik.kt_blps.api.chat

data class PagedChatMessagesResponse(
    val items: List<ChatMessageResponse>,
    val total: Long,
    val limit: Int,
    val offset: Long
)
