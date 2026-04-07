package ru.chousik.blps_kt.service.payment

import com.fasterxml.jackson.databind.JsonNode
import java.time.OffsetDateTime
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate
import org.springframework.web.server.ResponseStatusException
import ru.chousik.blps_kt.model.ExtraServiceRequestStatus
import ru.chousik.blps_kt.model.PaymentRequestStatus
import ru.chousik.blps_kt.repository.ExtraServiceRequestRepository
import ru.chousik.blps_kt.repository.PaymentRequestRepository
import ru.chousik.blps_kt.service.ChatSystemMessageService

@Service
class YooKassaNotificationProcessor(
    private val yooKassaClient: YooKassaClient,
    private val paymentRequestRepository: PaymentRequestRepository,
    private val extraServiceRequestRepository: ExtraServiceRequestRepository,
    private val chatSystemMessageService: ChatSystemMessageService,
    @Qualifier("jtaTransactionTemplate")
    private val transactionTemplate: TransactionTemplate
) {

    fun process(root: JsonNode) {
        val type = root.path("type").asText(null)
            ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "type is required")
        if (type != "notification") {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "unsupported webhook type")
        }

        val event = root.path("event").asText(null)
            ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "event is required")
        val paymentObject = root.path("object")
        if (paymentObject.isMissingNode) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "object is required")
        }

        val providerPaymentId = paymentObject.path("id").asText(null)
            ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "payment object id is required")
        val providerStatus = paymentObject.path("status").asText("")
        val canonicalPayment = yooKassaClient.getPayment(providerPaymentId)

        if (canonicalPayment.id != providerPaymentId) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "payment id mismatch")
        }
        if (providerStatus.isNotBlank() && providerStatus != canonicalPayment.status) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "webhook payment status mismatch")
        }

        val payment = paymentRequestRepository.findByProviderPaymentId(providerPaymentId)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "payment request not found for provider payment id")
        val extraService = extraServiceRequestRepository.findById(payment.extraServiceRequestId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "extra service not found") }

        val metadataPaymentRequestId = canonicalPayment.metadata["paymentRequestId"]
        val metadataExtraServiceRequestId = canonicalPayment.metadata["extraServiceRequestId"]
        if (metadataPaymentRequestId != payment.id.toString()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "paymentRequestId metadata mismatch")
        }
        if (metadataExtraServiceRequestId != extraService.id.toString()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "extraServiceRequestId metadata mismatch")
        }
        if (canonicalPayment.amountValue.compareTo(extraService.amount) != 0) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "payment amount mismatch")
        }
        if (!canonicalPayment.amountCurrency.equals(extraService.currency, ignoreCase = true)) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "payment currency mismatch")
        }

        transactionTemplate.executeWithoutResult {
            when (canonicalPayment.status) {
                "succeeded" -> {
                    if (event != "payment.succeeded") {
                        throw ResponseStatusException(HttpStatus.BAD_REQUEST, "event does not match payment status")
                    }
                    markPaid(payment, extraService)
                }

                "canceled" -> {
                    if (event != "payment.canceled") {
                        throw ResponseStatusException(HttpStatus.BAD_REQUEST, "event does not match payment status")
                    }
                    markFailed(payment, extraService)
                }

                "pending" -> {
                    if (payment.status != PaymentRequestStatus.PENDING) {
                        payment.status = PaymentRequestStatus.PENDING
                        payment.expiresAt = canonicalPayment.expiresAt
                        paymentRequestRepository.save(payment)
                    }
                }

                "waiting_for_capture" -> {
                    throw ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "unexpected YooKassa status waiting_for_capture for capture=true flow"
                    )
                }

                else -> {
                    throw ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "unsupported YooKassa payment status '${canonicalPayment.status}'"
                    )
                }
            }
        }
    }

    private fun markPaid(
        payment: ru.chousik.blps_kt.model.PaymentRequest,
        extraService: ru.chousik.blps_kt.model.ExtraServiceRequest
    ) {
        if (payment.status == PaymentRequestStatus.PAID && extraService.status == ExtraServiceRequestStatus.PAID) {
            return
        }

        val now = OffsetDateTime.now()
        payment.status = PaymentRequestStatus.PAID
        payment.resolvedAt = now
        extraService.status = ExtraServiceRequestStatus.PAID
        extraService.updatedAt = now

        paymentRequestRepository.save(payment)
        extraServiceRequestRepository.save(extraService)
        chatSystemMessageService.append(
            extraService.chat,
            "Payment for extra service '${extraService.title}' succeeded."
        )
    }

    private fun markFailed(
        payment: ru.chousik.blps_kt.model.PaymentRequest,
        extraService: ru.chousik.blps_kt.model.ExtraServiceRequest
    ) {
        if (payment.status == PaymentRequestStatus.FAILED &&
            extraService.status == ExtraServiceRequestStatus.PAYMENT_FAILED
        ) {
            return
        }

        val now = OffsetDateTime.now()
        payment.status = PaymentRequestStatus.FAILED
        payment.resolvedAt = now
        extraService.status = ExtraServiceRequestStatus.PAYMENT_FAILED
        extraService.updatedAt = now

        paymentRequestRepository.save(payment)
        extraServiceRequestRepository.save(extraService)
        chatSystemMessageService.append(
            extraService.chat,
            "Payment for extra service '${extraService.title}' failed or was canceled."
        )
    }
}
