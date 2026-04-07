package ru.chousik.kt_blps.dto.chat

data class PagedChatsResponse(
    val items: List<ChatResponse>,
    val total: Long,
    val limit: Int,
    val offset: Long
)
