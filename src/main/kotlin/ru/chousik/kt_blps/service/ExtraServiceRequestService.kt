package ru.chousik.blps_kt.service

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.OffsetDateTime
import java.util.UUID
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.domain.Page
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionTemplate
import org.springframework.web.server.ResponseStatusException
import ru.chousik.blps_kt.api.extraservice.ExtraServiceRequestCreateDTO
import ru.chousik.blps_kt.api.extraservice.ExtraServiceRequestResponseDTO
import ru.chousik.blps_kt.api.extraservice.ExtraServiceRequestUpdateDTO
import ru.chousik.blps_kt.api.payment.ExtraServiceDecision
import ru.chousik.blps_kt.api.payment.ExtraServiceDecisionRequest
import ru.chousik.blps_kt.api.payment.ExtraServiceDecisionResponse
import ru.chousik.blps_kt.api.payment.PaymentRequestView
import ru.chousik.blps_kt.model.Chat
import ru.chousik.blps_kt.model.ExtraServiceRequest
import ru.chousik.blps_kt.model.ExtraServiceRequestStatus
import ru.chousik.blps_kt.model.PaymentRequest
import ru.chousik.blps_kt.model.PaymentRequestStatus
import ru.chousik.blps_kt.model.User
import ru.chousik.blps_kt.model.UserRole
import ru.chousik.blps_kt.pagination.OffsetBasedPageRequest
import ru.chousik.blps_kt.repository.ChatRepository
import ru.chousik.blps_kt.repository.ExtraServiceRequestRepository
import ru.chousik.blps_kt.repository.PaymentRequestRepository
import ru.chousik.blps_kt.repository.UserRepository
import ru.chousik.blps_kt.security.CurrentAccountService
import ru.chousik.blps_kt.service.payment.YooKassaClient
import ru.chousik.blps_kt.service.payment.YooKassaCreatePaymentResult

@Service
class ExtraServiceRequestService(
    private val chatRepository: ChatRepository,
    private val userRepository: UserRepository,
    private val extraServiceRequestRepository: ExtraServiceRequestRepository,
    private val paymentRequestRepository: PaymentRequestRepository,
    private val chatSystemMessageService: ChatSystemMessageService,
    private val yooKassaClient: YooKassaClient,
    private val currentAccountService: CurrentAccountService,
    @Qualifier("jtaTransactionTemplate")
    private val transactionTemplate: TransactionTemplate,
    @Qualifier("jtaRequiresNewTransactionTemplate")
    private val requiresNewTransactionTemplate: TransactionTemplate
) {

    fun createExtraService(
        chatId: UUID,
        dto: ExtraServiceRequestCreateDTO
    ): ExtraServiceRequestResponseDTO {
        val requesterId = currentAccountService.currentAccount().userId
        val createdService = transactionTemplate.execute {
            val chat = loadChat(chatId)
            val requester = loadUser(requesterId)

            ensureHostRequester(chat, requester)

            val now = OffsetDateTime.now()
            val entity = ExtraServiceRequest().apply {
                id = UUID.randomUUID()
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
            ExtraServiceRequestResponseDTO.from(saved)
        }
        return requireNotNull(createdService) { "extra service creation transaction returned null result" }
    }

    @Transactional(readOnly = true)
    fun getExtraServicesForChat(
        chatId: UUID,
        limit: Int,
        offset: Long
    ): Page<ExtraServiceRequestResponseDTO> {
        val requesterId = currentAccountService.currentAccount().userId
        val chat = loadChat(chatId)
        val requester = loadUser(requesterId)
        ensureParticipantOrAdmin(chat, requester)

        val pageable = OffsetBasedPageRequest(limit, offset, Sort.by(Sort.Direction.DESC, "createdAt"))
        return extraServiceRequestRepository.findAllByChatId(chatId, pageable)
            .map { ExtraServiceRequestResponseDTO.from(it) }
    }

    @Transactional(readOnly = true)
    fun getExtraService(serviceId: UUID): ExtraServiceRequestResponseDTO {
        val requesterId = currentAccountService.currentAccount().userId
        val service = loadExtraService(serviceId)
        val requester = loadUser(requesterId)
        ensureParticipantOrAdmin(service.chat, requester)
        return ExtraServiceRequestResponseDTO.from(service)
    }

    @Transactional(readOnly = true)
    fun getExtraServicePayment(serviceId: UUID): PaymentRequestView {
        val requesterId = currentAccountService.currentAccount().userId
        val service = loadExtraService(serviceId)
        val requester = loadUser(requesterId)
        ensureParticipantOrAdmin(service.chat, requester)

        val payment = paymentRequestRepository.findFirstByExtraServiceRequestIdOrderByCreatedAtDesc(service.id)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "payment request not found for extra service")

        return PaymentRequestView.from(payment)
    }

    fun updateExtraService(
        serviceId: UUID,
        dto: ExtraServiceRequestUpdateDTO
    ): ExtraServiceRequestResponseDTO {
        val requesterId = currentAccountService.currentAccount().userId
        val updatedService = transactionTemplate.execute {
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
        return requireNotNull(updatedService) { "extra service update transaction returned null result" }
    }

    fun deleteExtraService(serviceId: UUID) {
        val requesterId = currentAccountService.currentAccount().userId
        transactionTemplate.executeWithoutResult {
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
        request: ExtraServiceDecisionRequest
    ): ExtraServiceDecisionResponse =
        when (request.decision!!) {
            ExtraServiceDecision.REJECT -> rejectExtraService(serviceId)
            ExtraServiceDecision.ACCEPT -> acceptExtraService(serviceId)
        }

    private fun rejectExtraService(serviceId: UUID): ExtraServiceDecisionResponse {
        val requesterId = currentAccountService.currentAccount().userId
        val rejectedService = transactionTemplate.execute {
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
            ExtraServiceDecisionResponse(extraService = ExtraServiceRequestResponseDTO.from(savedService))
        }
        return requireNotNull(rejectedService) { "extra service reject transaction returned null result" }
    }

    private fun acceptExtraService(serviceId: UUID): ExtraServiceDecisionResponse {
        val requesterId = currentAccountService.currentAccount().userId
        val prepared = requireNotNull(
            transactionTemplate.execute {
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
                val paymentRequestId = UUID.randomUUID()
                val payment = PaymentRequest().apply {
                    id = paymentRequestId
                    extraServiceRequestId = service.id
                    initiatedBy = requester
                    status = PaymentRequestStatus.WAITING_PAYMENT
                    createdAt = now
                }

                service.status = ExtraServiceRequestStatus.PAYMENT_LINK_SENT
                service.updatedAt = now

                extraServiceRequestRepository.save(service)
                paymentRequestRepository.save(payment)
                touchChat(service.chat, now)

                PreparedAcceptance(
                    paymentRequestId = paymentRequestId,
                    serviceId = service.id,
                    guestUserId = requester.id,
                    chatId = service.chat.id,
                    title = service.title,
                    amount = service.amount,
                    currency = service.currency
                )
            }
        ) { "extra service accept preparation transaction returned null result" }

        val providerPayment = try {
            yooKassaClient.createPayment(
                paymentRequestId = prepared.paymentRequestId,
                extraServiceRequestId = prepared.serviceId,
                chatId = prepared.chatId,
                title = prepared.title,
                amount = prepared.amount,
                currency = prepared.currency,
                guestUserId = prepared.guestUserId
            )
        } catch (ex: Exception) {
            markPaymentCreationFailed(prepared)
            throw ex
        }

        val acceptedService = requiresNewTransactionTemplate.execute {
            finalizeAcceptedExtraService(prepared, providerPayment)
        }
        return requireNotNull(acceptedService) { "extra service accept finalize transaction returned null result" }
    }

    private fun markPaymentCreationFailed(prepared: PreparedAcceptance) {
        requiresNewTransactionTemplate.executeWithoutResult {
            val service = loadExtraService(prepared.serviceId)
            val payment = paymentRequestRepository.findById(prepared.paymentRequestId).orElse(null)
                ?: return@executeWithoutResult

            val now = OffsetDateTime.now()
            payment.status = PaymentRequestStatus.FAILED
            payment.resolvedAt = now
            service.status = ExtraServiceRequestStatus.PAYMENT_FAILED
            service.updatedAt = now

            paymentRequestRepository.save(payment)
            extraServiceRequestRepository.save(service)
            touchChat(service.chat, now)
            chatSystemMessageService.append(
                chat = service.chat,
                message = "Payment link creation for extra service '${service.title}' failed."
            )
        }
    }

    private fun finalizeAcceptedExtraService(
        prepared: PreparedAcceptance,
        providerPayment: YooKassaCreatePaymentResult
    ): ExtraServiceDecisionResponse {
        val service = loadExtraService(prepared.serviceId)
        val payment = paymentRequestRepository.findById(prepared.paymentRequestId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "payment request not found") }

        val now = OffsetDateTime.now()
        payment.providerPaymentId = providerPayment.providerPaymentId
        payment.paymentUrl = providerPayment.paymentUrl
        payment.status = mapProviderPaymentStatus(providerPayment.providerStatus)
        payment.expiresAt = providerPayment.expiresAt
        payment.resolvedAt = if (payment.status == PaymentRequestStatus.PAID || payment.status == PaymentRequestStatus.FAILED) {
            now
        } else {
            null
        }

        service.status = mapExtraServiceStatus(providerPayment.providerStatus)
        service.updatedAt = now

        val savedPayment = paymentRequestRepository.save(payment)
        val savedService = extraServiceRequestRepository.save(service)
        touchChat(service.chat, now)
        chatSystemMessageService.append(
            chat = service.chat,
            message = "Guest accepted extra service '${savedService.title}'. Payment request created."
        )

        return ExtraServiceDecisionResponse(
            extraService = ExtraServiceRequestResponseDTO.from(savedService),
            payment = PaymentRequestView.from(savedPayment)
        )
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
            requester.id == chat.host.id || requester.id == chat.guest.id
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

    private fun mapProviderPaymentStatus(providerStatus: String): PaymentRequestStatus =
        when (providerStatus.lowercase()) {
            "pending" -> PaymentRequestStatus.PENDING
            "succeeded" -> PaymentRequestStatus.PAID
            "canceled" -> PaymentRequestStatus.FAILED
            else -> PaymentRequestStatus.WAITING_PAYMENT
        }

    private fun mapExtraServiceStatus(providerStatus: String): ExtraServiceRequestStatus =
        when (providerStatus.lowercase()) {
            "pending" -> ExtraServiceRequestStatus.PAYMENT_LINK_SENT
            "succeeded" -> ExtraServiceRequestStatus.PAID
            "canceled" -> ExtraServiceRequestStatus.PAYMENT_FAILED
            else -> ExtraServiceRequestStatus.PAYMENT_LINK_SENT
        }

    private data class PreparedAcceptance(
        val paymentRequestId: UUID,
        val serviceId: UUID,
        val guestUserId: UUID,
        val chatId: UUID,
        val title: String,
        val amount: BigDecimal,
        val currency: String
    )
}
