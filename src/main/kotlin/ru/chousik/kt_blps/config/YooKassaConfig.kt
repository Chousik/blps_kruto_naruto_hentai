package ru.chousik.blps_kt.config

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.web.client.RestClient

@Configuration
@EnableConfigurationProperties(YooKassaProperties::class)
class YooKassaConfig {

    @Bean
    fun yooKassaRestClient(
        properties: YooKassaProperties
    ): RestClient = RestClient.builder()
        .baseUrl(properties.apiBaseUrl)
        .defaultHeaders { headers ->
            headers.contentType = MediaType.APPLICATION_JSON
            headers.accept = listOf(MediaType.APPLICATION_JSON)
            if (properties.shopId.isNotBlank() && properties.secretKey.isNotBlank()) {
                headers.setBasicAuth(properties.shopId, properties.secretKey)
            }
            headers.add(HttpHeaders.USER_AGENT, "blps-kt-yookassa-client")
        }
        .build()
}
