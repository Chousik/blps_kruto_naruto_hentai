package ru.chousik.kt_blps.api.extraservice

data class PagedExtraServiceRequestsResponse(
    val items: List<ExtraServiceRequestResponseDTO>,
    val total: Long,
    val limit: Int,
    val offset: Long
)
