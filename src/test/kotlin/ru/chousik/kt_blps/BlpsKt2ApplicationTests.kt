package ru.chousik.kt_blps

import jakarta.resource.cci.ConnectionFactory
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.test.context.TestPropertySource

@SpringBootTest
@Import(BlpsKt2ApplicationTests.TestConfig::class)
@TestPropertySource(properties = ["spring.main.allow-bean-definition-overriding=true"])
class BlpsKt2ApplicationTests {

    @Test
    fun contextLoads() {
    }

    @TestConfiguration
    class TestConfig {
        @Bean("erpNextConnectionFactory")
        fun erpNextConnectionFactory(): ConnectionFactory =
            org.mockito.Mockito.mock(ConnectionFactory::class.java)
    }
}
