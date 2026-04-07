package ru.chousik.blps_kt.api.chat

data class PagedChatsResponse(
    val items: List<ChatResponse>,
    val total: Long,
    val limit: Int,
    val offset: Long
)
