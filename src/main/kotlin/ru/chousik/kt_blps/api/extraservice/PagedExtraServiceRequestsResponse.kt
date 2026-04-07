package ru.chousik.blps_kt.api.extraservice

data class PagedExtraServiceRequestsResponse(
    val items: List<ExtraServiceRequestResponseDTO>,
    val total: Long,
    val limit: Int,
    val offset: Long
)
