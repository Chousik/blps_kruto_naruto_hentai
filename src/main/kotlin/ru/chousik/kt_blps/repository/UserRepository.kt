package ru.chousik.kt_blps.repository

import java.util.UUID
import org.springframework.data.jpa.repository.JpaRepository
import ru.chousik.kt_blps.model.User

interface UserRepository : JpaRepository<User, UUID> {
    fun existsByUsername(username: String): Boolean

    fun existsByEmail(email: String): Boolean
}
