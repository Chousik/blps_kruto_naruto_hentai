package ru.chousik.kt_blps.service

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Service
import ru.chousik.kt_blps.dto.payment.PaymentCreationFailedEvent
import ru.chousik.kt_blps.dto.payment.PaymentUrlAssignedEvent

@Service
class PaymentUrlAssignedConsumerService(
    private val objectMapper: ObjectMapper,
    private val extraServiceRequestService: ExtraServiceRequestService,
    private val erpSyncOutboxService: ErpSyncOutboxService
) {
    @KafkaListener(topics = ["\${app.kafka.payment-url-assigned-topic}"])
    fun consume(payload: String) {
        val root = objectMapper.readTree(payload)
        when {
            root.hasNonNull("paymentRequestId") -> {
                val event = objectMapper.treeToValue(root, PaymentUrlAssignedEvent::class.java)
                extraServiceRequestService.markPaymentLinkAssigned(event.extraServiceRequestId)
                erpSyncOutboxService.enqueueSyncSalesInvoiceForExtraService(event.extraServiceRequestId)
            }

            root.hasNonNull("extraServiceRequestId") && root.has("errorMessage") -> {
                val event = objectMapper.treeToValue(root, PaymentCreationFailedEvent::class.java)
                extraServiceRequestService.markPaymentCreationFailed(event.extraServiceRequestId, event.errorMessage)
            }
        }
    }
}
