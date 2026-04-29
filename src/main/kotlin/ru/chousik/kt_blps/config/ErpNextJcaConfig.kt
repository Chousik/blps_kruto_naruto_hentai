package ru.chousik.kt_blps.config

import jakarta.resource.cci.ConnectionFactory
import javax.naming.InitialContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class ErpNextJcaConfig {
    @Bean
    fun erpNextConnectionFactory(
        erpNextProperties: ErpNextProperties
    ): ConnectionFactory {
        val jndiName = erpNextProperties.jndiName.trim()
        if (jndiName.isBlank()) {
            throw IllegalStateException("ERPNext JNDI name is not configured")
        }

        val lookedUp = InitialContext.doLookup<Any>(jndiName)
        return lookedUp as? ConnectionFactory
            ?: throw IllegalStateException("JNDI resource '$jndiName' is not a jakarta.resource.cci.ConnectionFactory")
    }
}
