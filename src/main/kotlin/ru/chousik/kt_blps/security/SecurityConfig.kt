package ru.chousik.blps_kt.security

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.authentication.HttpStatusEntryPoint
import org.springframework.security.web.context.HttpSessionSecurityContextRepository
import org.springframework.security.web.context.SecurityContextRepository
import org.springframework.security.web.SecurityFilterChain

@Configuration
@EnableMethodSecurity
class SecurityConfig(
    private val jaasXmlAuthenticationProvider: JaasXmlAuthenticationProvider
) {

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    @Bean
    fun securityContextRepository(): SecurityContextRepository =
        HttpSessionSecurityContextRepository().apply {
            setDisableUrlRewriting(true)
            setAllowSessionCreation(true)
        }

    @Bean
    fun securityFilterChain(
        http: HttpSecurity,
        securityContextRepository: SecurityContextRepository
    ): SecurityFilterChain =
        http
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED) }
            .securityContext { it.securityContextRepository(securityContextRepository) }
            .authenticationProvider(jaasXmlAuthenticationProvider)
            .httpBasic(Customizer.withDefaults())
            .exceptionHandling { it.authenticationEntryPoint(HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)) }
            .authorizeHttpRequests {
                it.requestMatchers(
                    "/actuator/health",
                    "/webhooks/yookassa/**",
                    "/error",
                    "/rest-endpoints-smoke.html",
                    "/auth/register",
                    "/ws",
                    "/ws/**"
                ).permitAll()
                    .anyRequest().authenticated()
            }
            .build()
}
