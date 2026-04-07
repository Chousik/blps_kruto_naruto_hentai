package ru.chousik.blps_kt.service.payment

import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.server.ResponseStatusException
import ru.chousik.blps_kt.config.YooKassaProperties

@Component
class YooKassaClient(
    private val yooKassaRestClient: RestClient,
    private val yooKassaProperties: YooKassaProperties
) {

    fun createPayment(
        paymentRequestId: UUID,
        extraServiceRequestId: UUID,
        chatId: UUID,
        title: String,
        amount: BigDecimal,
        currency: String,
        guestUserId: UUID
    ): YooKassaCreatePaymentResult {
        ensureConfigured()

        val response = yooKassaRestClient.post()
            .uri("/v3/payments")
            .header("Idempotence-Key", paymentRequestId.toString())
            .body(
                YooKassaCreatePaymentRequest(
                    amount = AmountPayload(
                        value = amount.toPlainString(),
                        currency = currency
                    ),
                    capture = true,
                    confirmation = ConfirmationPayload(
                        type = "redirect",
                        returnUrl = yooKassaProperties.returnUrl
                    ),
                    description = "Extra service '$title' for chat $chatId",
                    metadata = mapOf(
                        "paymentRequestId" to paymentRequestId.toString(),
                        "extraServiceRequestId" to extraServiceRequestId.toString(),
                        "chatId" to chatId.toString(),
                        "guestUserId" to guestUserId.toString()
                    )
                )
            )
            .retrieve()
            .body(YooKassaCreatePaymentResponse::class.java)
            ?: throw ResponseStatusException(HttpStatus.BAD_GATEWAY, "empty response from YooKassa")

        return YooKassaCreatePaymentResult(
            providerPaymentId = response.id,
            providerStatus = response.status,
            paymentUrl = response.confirmation?.confirmationUrl,
            expiresAt = response.expiresAt
        )
    }

    fun getPayment(paymentId: String): YooKassaPaymentDetails {
        ensureConfigured()

        val response = yooKassaRestClient.get()
            .uri("/v3/payments/{paymentId}", paymentId)
            .retrieve()
            .body(YooKassaPaymentResponse::class.java)
            ?: throw ResponseStatusException(HttpStatus.BAD_GATEWAY, "empty payment response from YooKassa")

        return YooKassaPaymentDetails(
            id = response.id,
            status = response.status,
            amountValue = response.amount.value,
            amountCurrency = response.amount.currency,
            paymentUrl = response.confirmation?.confirmationUrl,
            expiresAt = response.expiresAt,
            metadata = response.metadata ?: emptyMap()
        )
    }

    private fun ensureConfigured() {
        if (yooKassaProperties.shopId.isBlank() || yooKassaProperties.secretKey.isBlank()) {
            throw ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "YooKassa credentials are not configured"
            )
        }
    }
}

data class YooKassaCreatePaymentResult(
    val providerPaymentId: String,
    val providerStatus: String,
    val paymentUrl: String?,
    val expiresAt: OffsetDateTime?
)

data class YooKassaPaymentDetails(
    val id: String,
    val status: String,
    val amountValue: BigDecimal,
    val amountCurrency: String,
    val paymentUrl: String?,
    val expiresAt: OffsetDateTime?,
    val metadata: Map<String, String>
)

data class YooKassaCreatePaymentRequest(
    val amount: AmountPayload,
    val capture: Boolean,
    val confirmation: ConfirmationPayload,
    val description: String,
    val metadata: Map<String, String>
)

data class AmountPayload(
    val value: String,
    val currency: String
)

data class ConfirmationPayload(
    val type: String,
    @com.fasterxml.jackson.annotation.JsonProperty("return_url")
    val returnUrl: String
)

data class YooKassaCreatePaymentResponse(
    val id: String,
    val status: String,
    val confirmation: YooKassaConfirmationResponse?,
    @com.fasterxml.jackson.annotation.JsonProperty("expires_at")
    val expiresAt: OffsetDateTime?
)

data class YooKassaPaymentResponse(
    val id: String,
    val status: String,
    val amount: YooKassaAmountResponse,
    val confirmation: YooKassaConfirmationResponse?,
    @com.fasterxml.jackson.annotation.JsonProperty("expires_at")
    val expiresAt: OffsetDateTime?,
    val metadata: Map<String, String>?
)

data class YooKassaAmountResponse(
    val value: BigDecimal,
    val currency: String
)

data class YooKassaConfirmationResponse(
    val type: String?,
    @com.fasterxml.jackson.annotation.JsonProperty("confirmation_url")
    val confirmationUrl: String?
)
