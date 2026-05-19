package ru.chousik.kt_blps.service

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Service
import ru.chousik.kt_blps.dto.payment.PaymentCreationFailedEvent
import ru.chousik.kt_blps.dto.payment.PaymentUrlAssignedEvent
import ru.chousik.kt_blps.workflow.ExtraServiceWorkflowService

@Service
class PaymentUrlAssignedConsumerService(
    private val objectMapper: ObjectMapper,
    private val extraServiceWorkflowService: ExtraServiceWorkflowService
) {
    @KafkaListener(topics = ["\${app.kafka.payment-url-assigned-topic}"])
    fun consume(payload: String) {
        val root = objectMapper.readTree(payload)
        when {
            root.hasNonNull("paymentRequestId") -> {
                val event = objectMapper.treeToValue(root, PaymentUrlAssignedEvent::class.java)
                extraServiceWorkflowService.notifyPaymentLinkAssigned(event.extraServiceRequestId, event.paymentRequestId)
            }

            root.hasNonNull("extraServiceRequestId") && root.has("errorMessage") -> {
                val event = objectMapper.treeToValue(root, PaymentCreationFailedEvent::class.java)
                extraServiceWorkflowService.notifyPaymentCreationFailed(event.extraServiceRequestId, event.errorMessage)
            }
        }
    }
}
