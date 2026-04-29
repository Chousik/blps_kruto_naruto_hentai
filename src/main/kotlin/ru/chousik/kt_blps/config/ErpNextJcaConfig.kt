package ru.chousik.kt_blps.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ru.chousik.kt_blps.jca.erpnext.ErpNextJcaConnectionFactory
import tools.jackson.databind.ObjectMapper

@Configuration
class ErpNextJcaConfig {
    @Bean
    fun erpNextConnectionFactory(
        erpNextProperties: ErpNextProperties,
        objectMapper: ObjectMapper
    ): ErpNextJcaConnectionFactory = ErpNextJcaConnectionFactory(erpNextProperties, objectMapper)
}
