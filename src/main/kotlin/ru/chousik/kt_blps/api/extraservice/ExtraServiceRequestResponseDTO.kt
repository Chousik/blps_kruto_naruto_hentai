package ru.chousik.blps_kt.api.extraservice

import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID
import ru.chousik.blps_kt.model.ExtraServiceRequest
import ru.chousik.blps_kt.model.ExtraServiceRequestStatus

data class ExtraServiceRequestResponseDTO(
    val id: UUID,
    val chatId: UUID,
    val requestedByUserId: UUID,
    val status: ExtraServiceRequestStatus,
    val title: String,
    val description: String,
    val amount: BigDecimal,
    val currency: String,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime
) {
    companion object {
        fun from(entity: ExtraServiceRequest): ExtraServiceRequestResponseDTO =
            ExtraServiceRequestResponseDTO(
                id = entity.id,
                chatId = entity.chat.id,
                requestedByUserId = entity.chat.host.id,
                status = entity.status,
                title = entity.title,
                description = entity.description,
                amount = entity.amount,
                currency = entity.currency,
                createdAt = entity.createdAt,
                updatedAt = entity.updatedAt
            )
    }
}
