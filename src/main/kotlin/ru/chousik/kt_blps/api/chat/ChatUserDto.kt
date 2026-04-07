package ru.chousik.blps_kt.api.chat

import java.util.UUID
import ru.chousik.blps_kt.model.User
import ru.chousik.blps_kt.model.UserRole

data class ChatUserDto(
    val id: UUID,
    val username: String,
    val role: UserRole
) {
    companion object {
        fun from(user: User): ChatUserDto = ChatUserDto(
            id = user.id,
            username = user.username,
            role = user.role
        )
    }
}
