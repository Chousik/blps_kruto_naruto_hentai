package ru.chousik.blps_kt.repository

import java.util.UUID
import org.springframework.data.jpa.repository.JpaRepository
import ru.chousik.blps_kt.model.User

interface UserRepository : JpaRepository<User, UUID> {
    fun existsByUsername(username: String): Boolean

    fun existsByEmail(email: String): Boolean
}
