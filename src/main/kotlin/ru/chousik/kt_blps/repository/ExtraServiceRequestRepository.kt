package ru.chousik.kt_blps.repository

import java.util.UUID
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import ru.chousik.kt_blps.model.ExtraServiceRequest

interface ExtraServiceRequestRepository : JpaRepository<ExtraServiceRequest, UUID> {
    fun findAllByChatId(chatId: UUID, pageable: Pageable): Page<ExtraServiceRequest>
}
