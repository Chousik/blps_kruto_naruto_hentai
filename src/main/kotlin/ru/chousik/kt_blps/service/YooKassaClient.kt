package ru.chousik.kt_blps.service

import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import ru.chousik.kt_blps.config.YooKassaProperties
import ru.chousik.kt_blps.dto.payment.YooKassaCreatePaymentRequest
import ru.chousik.kt_blps.dto.payment.YooKassaCreatePaymentResponse
import java.nio.charset.StandardCharsets
import java.util.Base64
import java.util.UUID

@Component
class YooKassaClient(
    private val properties: YooKassaProperties
) {
    private val client = RestClient.builder()
        .baseUrl(properties.apiUrl)
        .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .build()

    fun createPayment(request: YooKassaCreatePaymentRequest): YooKassaCreatePaymentResponse {
        return client.post()
            .uri("/payments")
            .header(HttpHeaders.AUTHORIZATION, buildAuthorizationHeader())
            .header("Idempotence-Key", UUID.randomUUID().toString())
            .body(request)
            .retrieve()
            .body(YooKassaCreatePaymentResponse::class.java)
            ?: throw IllegalStateException("Empty response from YooKassa")
    }

    private fun buildAuthorizationHeader(): String {
        val raw = "${properties.shopId}:${properties.secretKey}"
        val encoded = Base64.getEncoder()
            .encodeToString(raw.toByteArray(StandardCharsets.UTF_8))
        return "Basic $encoded"
    }
}
