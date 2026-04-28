package ru.chousik.payment_worker_service.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class UtilsConfig {
    @Bean
    fun objectMapper(): ObjectMapper = jacksonObjectMapper()
}
