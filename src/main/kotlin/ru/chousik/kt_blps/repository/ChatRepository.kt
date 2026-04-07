package ru.chousik.kt_blps.repository

import java.util.UUID
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import ru.chousik.kt_blps.model.Chat

interface ChatRepository : JpaRepository<Chat, UUID> {
    fun existsByGuestIdAndHostId(guestId: UUID, hostId: UUID): Boolean

    fun findAllByGuestIdOrHostId(guestId: UUID, hostId: UUID, pageable: Pageable): Page<Chat>
}
