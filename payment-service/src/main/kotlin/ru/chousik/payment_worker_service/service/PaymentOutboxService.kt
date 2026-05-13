package ru.chousik.payment_worker_service.service

import com.fasterxml.jackson.databind.ObjectMapper
import java.time.OffsetDateTime
import java.util.UUID
import org.springframework.stereotype.Service
import ru.chousik.payment_worker_service.event.PaymentCreationFailedEvent
import ru.chousik.payment_worker_service.event.PaymentUrlAssignedEvent
import ru.chousik.payment_worker_service.model.PaymentOutboxEvent
import ru.chousik.payment_worker_service.model.PaymentOutboxEventStatus
import ru.chousik.payment_worker_service.repository.PaymentOutboxEventRepository

@Service
class PaymentOutboxService(
    private val objectMapper: ObjectMapper,
    private val paymentOutboxEventRepository: PaymentOutboxEventRepository,
    private val paymentOutboxPublisher: PaymentOutboxPublisher,
    private val afterCommitExecutor: AfterCommitExecutor
) {

    fun enqueuePaymentUrlAssignedEvent(paymentRequestId: UUID, extraServiceRequestId: UUID) {
        enqueuePayload(
            PaymentUrlAssignedEvent(
                paymentRequestId = paymentRequestId,
                extraServiceRequestId = extraServiceRequestId
            )
        )
    }

    fun enqueuePaymentCreationFailedEvent(extraServiceRequestId: UUID, errorMessage: String?) {
        enqueuePayload(
            PaymentCreationFailedEvent(
                extraServiceRequestId = extraServiceRequestId,
                errorMessage = errorMessage?.take(1000)
            )
        )
    }

    private fun enqueuePayload(event: Any) {
        val payload = objectMapper.writeValueAsString(event)
        if (paymentOutboxEventRepository.existsByPayload(payload)) {
            return
        }

        val now = OffsetDateTime.now()
        val saved = paymentOutboxEventRepository.save(
            PaymentOutboxEvent().apply {
                this.payload = payload
                status = PaymentOutboxEventStatus.PENDING
                attemptCount = 0
                attemptedAt = now
                lastError = null
                createdAt = now
            }
        )

        afterCommitExecutor.run {
            paymentOutboxPublisher.publishNow(saved.id)
        }
    }
}
