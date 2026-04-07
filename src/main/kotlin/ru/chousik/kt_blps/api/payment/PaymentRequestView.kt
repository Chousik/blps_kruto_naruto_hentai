package ru.chousik.blps_kt.api.payment

import java.time.OffsetDateTime
import java.util.UUID
import ru.chousik.blps_kt.model.PaymentRequest
import ru.chousik.blps_kt.model.PaymentRequestStatus

data class PaymentRequestView(
    val id: UUID,
    val extraServiceRequestId: UUID,
    val initiatedByUserId: UUID,
    val providerPaymentId: String?,
    val paymentUrl: String?,
    val status: PaymentRequestStatus,
    val createdAt: OffsetDateTime,
    val expiresAt: OffsetDateTime?,
    val resolvedAt: OffsetDateTime?
) {
    companion object {
        fun from(entity: PaymentRequest): PaymentRequestView = PaymentRequestView(
            id = entity.id,
            extraServiceRequestId = entity.extraServiceRequestId,
            initiatedByUserId = entity.initiatedBy.id,
            providerPaymentId = entity.providerPaymentId,
            paymentUrl = entity.paymentUrl,
            status = entity.status,
            createdAt = entity.createdAt,
            expiresAt = entity.expiresAt,
            resolvedAt = entity.resolvedAt
        )
    }
}
