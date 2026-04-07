package ru.chousik.blps_kt.service

import java.time.OffsetDateTime
import java.util.UUID
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpStatus
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate
import org.springframework.web.server.ResponseStatusException
import ru.chousik.blps_kt.api.security.RegisterAccountRequest
import ru.chousik.blps_kt.api.security.RegisteredAccountResponse
import ru.chousik.blps_kt.model.User
import ru.chousik.blps_kt.model.UserRole
import ru.chousik.blps_kt.repository.UserRepository
import ru.chousik.blps_kt.security.XmlAccountDefinition
import ru.chousik.blps_kt.security.XmlAccountRegistry

@Service
class AccountRegistrationService(
    private val userRepository: UserRepository,
    private val xmlAccountRegistry: XmlAccountRegistry,
    private val passwordEncoder: PasswordEncoder,
    @Qualifier("jtaTransactionTemplate")
    private val transactionTemplate: TransactionTemplate,
    @Qualifier("jtaRequiresNewTransactionTemplate")
    private val requiresNewTransactionTemplate: TransactionTemplate
) {

    fun register(request: RegisterAccountRequest): RegisteredAccountResponse =
        xmlAccountRegistry.withWriteLock {
            val username = request.username!!.trim()
            val email = request.email!!.trim().lowercase()
            val password = request.password!!
            val role = request.role!!

            requirePublicRole(role)

            if (xmlAccountRegistry.findByUsername(username) != null || userRepository.existsByUsername(username)) {
                throw ResponseStatusException(HttpStatus.CONFLICT, "username is already taken")
            }

            if (userRepository.existsByEmail(email)) {
                throw ResponseStatusException(HttpStatus.CONFLICT, "email is already taken")
            }

            val userId = UUID.randomUUID()
            val now = OffsetDateTime.now()
            val response = requireNotNull(
                transactionTemplate.execute {
                    val user = User().apply {
                        id = userId
                        this.username = username
                        this.email = email
                        firstName = username.take(128)
                        lastName = role.name.lowercase().replaceFirstChar { it.uppercase() }.take(128)
                        this.role = role
                        createdAt = now
                        updatedAt = now
                    }

                    val savedUser = userRepository.save(user)
                    RegisteredAccountResponse(
                        userId = savedUser.id,
                        username = savedUser.username,
                        email = savedUser.email,
                        role = savedUser.role
                    )
                }
            ) { "registration transaction returned null result" }

            try {
                xmlAccountRegistry.appendAccount(
                    XmlAccountDefinition(
                        username = username,
                        passwordHash = requireNotNull(passwordEncoder.encode(password)) {
                            "password encoder returned null hash"
                        },
                        role = role,
                        userId = userId
                    )
                )
            } catch (ex: Exception) {
                requiresNewTransactionTemplate.executeWithoutResult {
                    userRepository.deleteById(userId)
                }
                throw ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "failed to persist account in XML storage",
                    ex
                )
            }

            response
        }

    private fun requirePublicRole(role: UserRole) {
        if (role == UserRole.ADMIN) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "self-registration as ADMIN is not allowed")
        }
    }
}
