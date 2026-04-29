package ru.chousik.kt_blps.service

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Service
import ru.chousik.kt_blps.dto.payment.PaymentUrlAssignedEvent

@Service
class PaymentUrlAssignedConsumerService(
    private val objectMapper: ObjectMapper,
    private val erpNextSyncService: ErpNextSyncService
) {
    @KafkaListener(topics = ["\${app.kafka.payment-url-assigned-topic}"])
    fun consume(payload: String) {
        val event = objectMapper.readValue(payload, PaymentUrlAssignedEvent::class.java)
        erpNextSyncService.syncSalesInvoiceForExtraService(event.extraServiceRequestId)
    }
}
