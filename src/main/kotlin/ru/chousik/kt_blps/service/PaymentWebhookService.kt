package ru.chousik.kt_blps.service

import com.fasterxml.jackson.databind.ObjectMapper
import java.time.OffsetDateTime
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate
import org.springframework.web.server.ResponseStatusException
import ru.chousik.kt_blps.dto.payment.YooKassaWebhookNotification
import ru.chousik.kt_blps.model.PaymentRequestStatus
import ru.chousik.kt_blps.repository.PaymentRequestRepository

@Service
class PaymentWebhookService(
    private val objectMapper: ObjectMapper,
    private val paymentRequestRepository: PaymentRequestRepository,
    private val afterCommitExecutor: AfterCommitExecutor,
    private val erpNextSyncService: ErpNextSyncService,
    @Qualifier("writeTransactionTemplate")
    private val writeTransactionTemplate: TransactionTemplate
) {

    fun handleWebhook(payload: String) {
        writeTransactionTemplate.executeWithoutResult {
            val notification = try {
                objectMapper.readValue(payload, YooKassaWebhookNotification::class.java)
            } catch (ex: Exception) {
                throw ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid webhook payload", ex)
            }

            if (notification.type != "notification") {
                throw ResponseStatusException(HttpStatus.BAD_REQUEST, "unsupported webhook type")
            }

            val providerPaymentId = notification.`object`.id
            val payment = paymentRequestRepository.findByProviderPaymentId(providerPaymentId)
                ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "payment request not found")

            when (notification.event) {
                "payment.succeeded" -> {
                    payment.status = PaymentRequestStatus.PAID
                    payment.resolvedAt = OffsetDateTime.now()
                    afterCommitExecutor.run("sync ERP payment entry for payment request ${payment.id}") {
                        erpNextSyncService.syncPaymentEntryForPaymentRequest(payment.id)
                    }
                }

                "payment.canceled" -> {
                    payment.status = PaymentRequestStatus.FAILED
                    payment.resolvedAt = OffsetDateTime.now()
                }

                "payment.waiting_for_capture" -> {
                    payment.status = PaymentRequestStatus.PENDING
                }

                else -> {
                    return@executeWithoutResult
                }
            }

            paymentRequestRepository.save(payment)
        }
    }
}
