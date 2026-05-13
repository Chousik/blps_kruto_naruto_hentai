package ru.chousik.kt_blps.service

import com.fasterxml.jackson.databind.ObjectMapper
import java.math.RoundingMode
import java.time.OffsetDateTime
import java.util.UUID
import org.springframework.beans.factory.annotation.Value
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.domain.Page
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate
import org.springframework.web.server.ResponseStatusException
import ru.chousik.kt_blps.dto.extraservice.ExtraServiceRequestCreateDTO
import ru.chousik.kt_blps.dto.extraservice.ExtraServiceRequestResponseDTO
import ru.chousik.kt_blps.dto.extraservice.ExtraServiceRequestUpdateDTO
import ru.chousik.kt_blps.dto.payment.ExtraServiceDecision
import ru.chousik.kt_blps.dto.payment.PaymentCreationRequestedEvent
import ru.chousik.kt_blps.dto.payment.ExtraServiceDecisionRequest
import ru.chousik.kt_blps.dto.payment.ExtraServiceDecisionResponse
import ru.chousik.kt_blps.dto.payment.PaymentRequestView
import ru.chousik.kt_blps.model.Chat
import ru.chousik.kt_blps.model.ExtraServiceRequest
import ru.chousik.kt_blps.model.ExtraServiceRequestStatus
import ru.chousik.kt_blps.model.User
import ru.chousik.kt_blps.model.UserRole
import ru.chousik.kt_blps.pagination.OffsetBasedPageRequest
import ru.chousik.kt_blps.repository.ChatRepository
import ru.chousik.kt_blps.repository.ExtraServiceRequestRepository
import ru.chousik.kt_blps.repository.PaymentRequestRepository
import ru.chousik.kt_blps.repository.UserRepository

@Service
class ExtraServiceRequestService(
    private val chatRepository: ChatRepository,
    private val userRepository: UserRepository,
    private val extraServiceRequestRepository: ExtraServiceRequestRepository,
    private val paymentRequestRepository: PaymentRequestRepository,
    private val chatSystemMessageService: ChatSystemMessageService,
    private val objectMapper: ObjectMapper,
    private val outboxService: OutboxService,
    private val erpSyncOutboxService: ErpSyncOutboxService,
    @Value("\${app.kafka.payment-topic}")
    private val paymentTopic: String,
    @Qualifier("writeTransactionTemplate")
    private val writeTransactionTemplate: TransactionTemplate,
    @Qualifier("readOnlyTransactionTemplate")
    private val readOnlyTransactionTemplate: TransactionTemplate,
) {

    fun createExtraService(
        chatId: UUID,
        requesterId: UUID,
        dto: ExtraServiceRequestCreateDTO
    ): ExtraServiceRequestResponseDTO = writeTransactionTemplate.execute {
        val chat = loadChat(chatId)
        val requester = loadUser(requesterId)

        ensureHostRequester(chat, requester)

        val now = OffsetDateTime.now()
        val entity = ExtraServiceRequest().apply {
            this.chat = chat
            status = ExtraServiceRequestStatus.WAITING_GUEST_APPROVAL
            title = dto.title!!.trim()
            description = dto.description!!.trim()
            amount = dto.amount!!.setScale(2, RoundingMode.HALF_UP)
            currency = dto.currency!!.uppercase()
            createdAt = now
            updatedAt = now
        }

        val saved = extraServiceRequestRepository.save(entity)
        touchChat(chat, now)
        chatSystemMessageService.append(
            chat = chat,
            message = "Host proposed extra service '${saved.title}' for ${saved.amount} ${saved.currency}."
        )
        erpSyncOutboxService.enqueueSyncQuotationForExtraService(saved.id)
        ExtraServiceRequestResponseDTO.from(saved)
    }

    fun getExtraServicesForChat(
        chatId: UUID,
        requesterId: UUID,
        limit: Int,
        offset: Long
    ): Page<ExtraServiceRequestResponseDTO> = readOnlyTransactionTemplate.execute {
        val chat = loadChat(chatId)
        val requester = loadUser(requesterId)
        ensureParticipantOrAdmin(chat, requester)

        val pageable = OffsetBasedPageRequest(limit, offset, Sort.by(Sort.Direction.DESC, "createdAt"))
        extraServiceRequestRepository.findAllByChatId(chatId, pageable)
            .map { ExtraServiceRequestResponseDTO.from(it) }
    }

    fun getExtraService(serviceId: UUID, requesterId: UUID): ExtraServiceRequestResponseDTO =
        readOnlyTransactionTemplate.execute<ExtraServiceRequestResponseDTO> {
            val service = loadExtraService(serviceId)
            val requester = loadUser(requesterId)
            ensureParticipantOrAdmin(service.chat, requester)
            ExtraServiceRequestResponseDTO.from(service)
        }

    fun getExtraServicePayment(serviceId: UUID, requesterId: UUID): PaymentRequestView =
        readOnlyTransactionTemplate.execute {
            val service = loadExtraService(serviceId)
            val requester = loadUser(requesterId)
            ensureParticipantOrAdmin(service.chat, requester)

            val payment = paymentRequestRepository
                .findFirstByExtraServiceRequestIdOrderByCreatedAtDesc(service.id)
                ?: throw ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    when (service.status) {
                        ExtraServiceRequestStatus.PAYMENT_LINK_REQUESTED -> "payment link is being created"
                        ExtraServiceRequestStatus.PAYMENT_FAILED -> "payment link creation failed"
                        else -> "payment request not found for extra service"
                    }
                )

            val view = PaymentRequestView.from(payment)
            val expiresAt = payment.expiresAt
            if (expiresAt != null && !expiresAt.isAfter(OffsetDateTime.now())) {
                view.copy(paymentUrl = null)
            } else {
                view
            }
        }

    fun updateExtraService(
        serviceId: UUID,
        requesterId: UUID,
        dto: ExtraServiceRequestUpdateDTO
    ): ExtraServiceRequestResponseDTO = writeTransactionTemplate.execute {
        val service = loadExtraService(serviceId)
        val requester = loadUser(requesterId)
        ensureHostOrAdmin(service.chat, requester)

        dto.title?.let { service.title = it.trim() }
        dto.description?.let { service.description = it.trim() }
        dto.amount?.let { service.amount = it.setScale(2, RoundingMode.HALF_UP) }
        dto.currency?.let { service.currency = it.uppercase() }
        dto.status?.let { service.status = it }

        val now = OffsetDateTime.now()
        service.updatedAt = now
        val savedService = extraServiceRequestRepository.save(service)
        touchChat(service.chat, now)
        chatSystemMessageService.append(
            chat = service.chat,
            message = "Extra service '${savedService.title}' was updated."
        )
        ExtraServiceRequestResponseDTO.from(savedService)
    }

    fun deleteExtraService(serviceId: UUID, requesterId: UUID) {
        writeTransactionTemplate.executeWithoutResult {
            val service = loadExtraService(serviceId)
            val requester = loadUser(requesterId)
            ensureHostOrAdmin(service.chat, requester)

            val title = service.title
            val chat = service.chat
            val now = OffsetDateTime.now()

            extraServiceRequestRepository.delete(service)
            touchChat(chat, now)
            chatSystemMessageService.append(
                chat = chat,
                message = "Extra service '$title' was deleted."
            )
        }
    }

    fun decideExtraService(
        serviceId: UUID,
        requesterId: UUID,
        request: ExtraServiceDecisionRequest
    ): ExtraServiceDecisionResponse = writeTransactionTemplate.execute {
        when (request.decision!!) {
            ExtraServiceDecision.REJECT -> rejectExtraService(serviceId, requesterId)
            ExtraServiceDecision.ACCEPT -> acceptExtraService(serviceId, requesterId)
        }
    }

    private fun rejectExtraService(serviceId: UUID, requesterId: UUID): ExtraServiceDecisionResponse {
        val service = loadExtraService(serviceId)
        val requester = loadUser(requesterId)
        ensureGuestDecisionAllowed(service, requester)
        ensureWaitingGuestApproval(service)

        val now = OffsetDateTime.now()
        service.status = ExtraServiceRequestStatus.REJECTED
        service.updatedAt = now
        val savedService = extraServiceRequestRepository.save(service)
        touchChat(service.chat, now)
        chatSystemMessageService.append(
            chat = savedService.chat,
            message = "Guest rejected extra service '${savedService.title}'."
        )
        return ExtraServiceDecisionResponse(extraService = ExtraServiceRequestResponseDTO.from(savedService))
    }

    private fun acceptExtraService(serviceId: UUID, requesterId: UUID): ExtraServiceDecisionResponse {
        val service = loadExtraService(serviceId)
        val requester = loadUser(requesterId)
        ensureGuestDecisionAllowed(service, requester)
        ensureWaitingGuestApproval(service)

        if (paymentRequestRepository.findFirstByExtraServiceRequestIdOrderByCreatedAtDesc(service.id) != null) {
            throw ResponseStatusException(
                HttpStatus.CONFLICT,
                "payment request for this extra service already exists"
            )
        }

        val now = OffsetDateTime.now()
        try {
            publishPaymentCreationRequested(
                PaymentCreationRequestedEvent(
                    extraServiceRequestId = service.id,
                    chatId = service.chat.id,
                    initiatedByUserId = requester.id,
                    title = service.title,
                    amount = service.amount.setScale(2, RoundingMode.HALF_UP).toPlainString(),
                    currency = service.currency.uppercase()
                )
            )
        } catch (ex: Exception) {
            throw ResponseStatusException(
                HttpStatus.BAD_GATEWAY,
                "failed to publish payment request event",
                ex
            )
        }

        service.status = ExtraServiceRequestStatus.PAYMENT_LINK_REQUESTED
        service.updatedAt = now

        val savedService = extraServiceRequestRepository.save(service)
        touchChat(service.chat, now)
        chatSystemMessageService.append(
            chat = service.chat,
            message = "Guest accepted extra service '${savedService.title}'. Payment link creation requested."
        )
        erpSyncOutboxService.enqueueSyncSalesOrderForExtraService(savedService.id)

        return ExtraServiceDecisionResponse(extraService = ExtraServiceRequestResponseDTO.from(savedService))
    }

    fun markPaymentLinkAssigned(serviceId: UUID) {
        writeTransactionTemplate.executeWithoutResult {
            val service = loadExtraService(serviceId)
            when (service.status) {
                ExtraServiceRequestStatus.PAYMENT_LINK_SENT,
                ExtraServiceRequestStatus.PAID,
                ExtraServiceRequestStatus.SERVICE_DELIVERED -> return@executeWithoutResult

                ExtraServiceRequestStatus.PAYMENT_LINK_REQUESTED,
                ExtraServiceRequestStatus.PAYMENT_FAILED -> Unit

                else -> return@executeWithoutResult
            }

            val now = OffsetDateTime.now()
            service.status = ExtraServiceRequestStatus.PAYMENT_LINK_SENT
            service.updatedAt = now
            val savedService = extraServiceRequestRepository.save(service)
            touchChat(savedService.chat, now)
            chatSystemMessageService.append(
                chat = savedService.chat,
                message = "Payment link for extra service '${savedService.title}' is ready."
            )
        }
    }

    fun markPaymentCreationFailed(serviceId: UUID, errorMessage: String?) {
        writeTransactionTemplate.executeWithoutResult {
            val service = loadExtraService(serviceId)
            when (service.status) {
                ExtraServiceRequestStatus.PAYMENT_LINK_SENT,
                ExtraServiceRequestStatus.PAID,
                ExtraServiceRequestStatus.SERVICE_DELIVERED -> return@executeWithoutResult

                ExtraServiceRequestStatus.PAYMENT_FAILED -> return@executeWithoutResult

                ExtraServiceRequestStatus.PAYMENT_LINK_REQUESTED -> Unit

                else -> return@executeWithoutResult
            }

            val now = OffsetDateTime.now()
            service.status = ExtraServiceRequestStatus.PAYMENT_FAILED
            service.updatedAt = now
            val savedService = extraServiceRequestRepository.save(service)
            touchChat(savedService.chat, now)
            val details = errorMessage?.take(180)?.takeIf { it.isNotBlank() }
            chatSystemMessageService.append(
                chat = savedService.chat,
                message = if (details == null) {
                    "Payment link creation failed for extra service '${savedService.title}'. Retry limit reached."
                } else {
                    "Payment link creation failed for extra service '${savedService.title}'. Retry limit reached. Reason: $details"
                }
            )
        }
    }

    private fun loadChat(chatId: UUID): Chat =
        chatRepository.findById(chatId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "chat not found") }

    private fun loadUser(userId: UUID): User =
        userRepository.findById(userId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "user not found") }

    private fun loadExtraService(serviceId: UUID): ExtraServiceRequest =
        extraServiceRequestRepository.findById(serviceId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "extra service not found") }

    private fun ensureHostRequester(chat: Chat, requester: User) {
        if (requester.role != UserRole.HOST || requester.id != chat.host.id) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "only host of the chat can create extra services")
        }
    }

    private fun ensureParticipantOrAdmin(chat: Chat, requester: User) {
        val allowed = requester.role == UserRole.ADMIN ||
                requester.id == chat.host.id ||
                requester.id == chat.guest.id
        if (!allowed) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "access denied to extra services")
        }
    }

    private fun ensureHostOrAdmin(chat: Chat, requester: User) {
        val allowed = (requester.role == UserRole.HOST && requester.id == chat.host.id) ||
                requester.role == UserRole.ADMIN
        if (!allowed) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "only host or admin can modify extra services")
        }
    }

    private fun ensureGuestDecisionAllowed(service: ExtraServiceRequest, requester: User) {
        val allowed = requester.role == UserRole.GUEST && requester.id == service.chat.guest.id
        if (!allowed) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "only guest of the chat can decide on extra service")
        }
    }

    private fun ensureWaitingGuestApproval(service: ExtraServiceRequest) {
        if (service.status != ExtraServiceRequestStatus.WAITING_GUEST_APPROVAL) {
            throw ResponseStatusException(
                HttpStatus.CONFLICT,
                "extra service decision is only allowed from WAITING_GUEST_APPROVAL status"
            )
        }
    }

    private fun touchChat(chat: Chat, at: OffsetDateTime) {
        chat.updatedAt = at
        chatRepository.save(chat)
    }

    private fun publishPaymentCreationRequested(event: PaymentCreationRequestedEvent) {
        val payload = objectMapper.writeValueAsString(event)
        outboxService.enqueue(
            aggregateType = "payment_creation_request",
            aggregateId = event.extraServiceRequestId,
            eventType = PaymentCreationRequestedEvent::class.qualifiedName ?: "PaymentCreationRequestedEvent",
            topic = paymentTopic,
            messageKey = event.extraServiceRequestId.toString(),
            payload = payload
        )
    }
}
