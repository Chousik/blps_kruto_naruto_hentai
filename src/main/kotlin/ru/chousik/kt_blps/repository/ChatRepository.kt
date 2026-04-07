package ru.chousik.blps_kt.repository

import java.util.UUID
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import ru.chousik.blps_kt.model.Chat

interface ChatRepository : JpaRepository<Chat, UUID> {
    fun existsByGuestIdAndHostId(guestId: UUID, hostId: UUID): Boolean

    fun findAllByGuestIdOrHostId(guestId: UUID, hostId: UUID, pageable: Pageable): Page<Chat>
}
