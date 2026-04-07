package ru.chousik.blps_kt.api.chat

data class PagedChatMessagesResponse(
    val items: List<ChatMessageResponse>,
    val total: Long,
    val limit: Int,
    val offset: Long
)
