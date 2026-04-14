package ru.chousik.kt_blps.service

import java.time.OffsetDateTime
import java.util.UUID
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import ru.chousik.kt_blps.dto.auth.RegisterRequest
import ru.chousik.kt_blps.dto.auth.RegisterResponse
import ru.chousik.kt_blps.model.User
import ru.chousik.kt_blps.model.UserRole
import ru.chousik.kt_blps.repository.UserRepository
import ru.chousik.kt_blps.security.XmlAccountDefinition
import ru.chousik.kt_blps.security.XmlAccountRegistry

@Service
class RegistrationService(
    private val userRepository: UserRepository,
    private val xmlAccountRegistry: XmlAccountRegistry,
    private val passwordEncoder: PasswordEncoder
) {

    fun register(request: RegisterRequest): RegisterResponse {
        val role = request.role
        if (role == UserRole.ADMIN) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "admin registration is not allowed")
        }

        val username = request.username.trim()
        val email = request.email.trim().lowercase()
        val password = request.password
        val firstName = request.firstName.trim()
        val lastName = request.lastName.trim()

        ensureUniqueCredentials(username, email)

        val now = OffsetDateTime.now()
        val user = User().apply {
            id = UUID.randomUUID()
            this.username = username
            this.email = email
            this.firstName = firstName
            this.lastName = lastName
            this.role = role
            createdAt = now
            updatedAt = now
        }

        val savedUser = try {
            userRepository.saveAndFlush(user)
        } catch (ex: DataIntegrityViolationException) {
            throw ResponseStatusException(
                HttpStatus.CONFLICT,
                "user with the same username or email already exists",
                ex
            )
        }

        val account = XmlAccountDefinition(
            username = savedUser.username,
            passwordHash = passwordEncoder.encode(password)!!,
            userId = savedUser.id,
            role = savedUser.role
        )

        try {
            xmlAccountRegistry.appendAccount(account)
        } catch (ex: IllegalStateException) {
            rollbackUser(savedUser.id, ex)
            throw ResponseStatusException(HttpStatus.CONFLICT, ex.message ?: "account already exists", ex)
        } catch (ex: Exception) {
            rollbackUser(savedUser.id, ex)
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "failed to create auth account", ex)
        }

        return RegisterResponse.from(savedUser)
    }

    private fun ensureUniqueCredentials(username: String, email: String) {
        if (xmlAccountRegistry.findByUsername(username) != null || userRepository.existsByUsername(username)) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "username is already taken")
        }
        if (userRepository.existsByEmail(email)) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "email is already taken")
        }
    }

    private fun rollbackUser(userId: UUID, cause: Exception) {
        try {
            userRepository.deleteById(userId)
        } catch (cleanupEx: Exception) {
            cause.addSuppressed(cleanupEx)
        }
    }
}
