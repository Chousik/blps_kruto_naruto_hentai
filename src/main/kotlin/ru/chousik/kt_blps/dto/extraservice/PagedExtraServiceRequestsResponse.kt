package ru.chousik.kt_blps.dto.extraservice

data class PagedExtraServiceRequestsResponse(
    val items: List<ExtraServiceRequestResponseDTO>,
    val total: Long,
    val limit: Int,
    val offset: Long
)
