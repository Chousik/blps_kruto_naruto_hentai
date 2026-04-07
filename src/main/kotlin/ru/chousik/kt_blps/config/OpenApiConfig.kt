package ru.chousik.blps_kt.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.servers.Server
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig {

    @Bean
    fun customOpenApi(): OpenAPI = OpenAPI().servers(
        listOf(
            Server()
                .url("https://fixly-meow.ru:8443")
                .description("Production HTTPS endpoint")
        )
    )
}
