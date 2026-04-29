package ru.chousik.kt_blps.service

import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate
import org.springframework.web.server.ResponseStatusException
import ru.chousik.kt_blps.config.ErpNextProperties
import ru.chousik.kt_blps.dto.erpnext.ErpNextCustomerRequest
import ru.chousik.kt_blps.dto.erpnext.ErpNextQuotationItemRequest
import ru.chousik.kt_blps.dto.erpnext.ErpNextQuotationRequest
import ru.chousik.kt_blps.jca.erpnext.ErpNextJcaConnection
import ru.chousik.kt_blps.jca.erpnext.ErpNextJcaConnectionFactory
import ru.chousik.kt_blps.jca.erpnext.ErpNextIntegrationException
import ru.chousik.kt_blps.model.ExtraServiceRequest
import ru.chousik.kt_blps.model.User
import ru.chousik.kt_blps.repository.ExtraServiceRequestRepository
import ru.chousik.kt_blps.repository.UserRepository

@Service
class ErpNextSyncService(
    private val erpNextConnectionFactory: ErpNextJcaConnectionFactory,
    private val erpNextProperties: ErpNextProperties,
    private val userRepository: UserRepository,
    private val extraServiceRequestRepository: ExtraServiceRequestRepository,
    @Qualifier("writeTransactionTemplate")
    private val writeTransactionTemplate: TransactionTemplate
) {

    fun syncCustomerForUser(userId: UUID) {
        writeTransactionTemplate.executeWithoutResult {
            val user = loadUser(userId)
            ensureCustomer(user)
        }
    }

    fun syncQuotationForExtraService(extraServiceId: UUID) {
        writeTransactionTemplate.executeWithoutResult {
            val extraService = loadExtraService(extraServiceId)
            if (!extraService.erpQuotationId.isNullOrBlank()) {
                return@executeWithoutResult
            }

            // Keep both participants represented in ERP.
            ensureCustomer(extraService.chat.host)
            val guestCustomer = ensureCustomer(extraService.chat.guest)

            val quotation = withConnection { connection ->
                connection.createQuotation(
                    ErpNextQuotationRequest(
                        company = requireCompany(),
                        partyName = guestCustomer.erpCustomerId!!,
                        transactionDate = extraService.createdAt.toLocalDate(),
                        items = listOf(
                            ErpNextQuotationItemRequest(
                                itemCode = requireServiceItemCode(),
                                qty = BigDecimal.ONE,
                                rate = extraService.amount,
                                description = buildServiceDescription(extraService)
                            )
                        ),
                        additionalFields = mapOf(
                            "custom_blps_extra_service_id" to extraService.id.toString(),
                            "custom_blps_chat_id" to extraService.chat.id.toString(),
                            "custom_blps_host_user_id" to extraService.chat.host.id.toString(),
                            "custom_blps_guest_user_id" to extraService.chat.guest.id.toString()
                        )
                    )
                )
            }

            extraService.erpQuotationId = quotation.name
            extraServiceRequestRepository.save(extraService)
        }
    }

    fun syncSalesOrderForAcceptedExtraService(extraServiceId: UUID) {
        writeTransactionTemplate.executeWithoutResult {
            val extraService = loadExtraService(extraServiceId)
            if (!extraService.erpSalesOrderId.isNullOrBlank()) {
                return@executeWithoutResult
            }
            ensureQuotation(extraService)
            val salesOrder = withConnection { connection ->
                connection.createSalesOrderFromQuotation(
                    extraService.erpQuotationId!!,
                    resolveDeliveryDate(extraService)
                )
            }
            extraService.erpSalesOrderId = salesOrder.name
            extraServiceRequestRepository.save(extraService)
        }
    }

    fun syncSalesInvoiceForExtraService(extraServiceId: UUID) {
        writeTransactionTemplate.executeWithoutResult {
            val extraService = loadExtraService(extraServiceId)
            if (!extraService.erpSalesInvoiceId.isNullOrBlank()) {
                return@executeWithoutResult
            }
            ensureSalesOrder(extraService)
            val salesInvoice = withConnection { connection ->
                connection.createSalesInvoiceFromSalesOrder(extraService.erpSalesOrderId!!)
            }
            extraService.erpSalesInvoiceId = salesInvoice.name
            extraServiceRequestRepository.save(extraService)
        }
    }

    private fun ensureCustomer(user: User): User {
        if (!user.erpCustomerId.isNullOrBlank()) {
            return user
        }

        val customer = withConnection { connection ->
            connection.ensureCustomer(
                ErpNextCustomerRequest(
                    customerName = buildCustomerName(user),
                    customerGroup = "Individual",
                    territory = "All Territories",
                    additionalFields = mapOf(
                        "custom_blps_user_id" to user.id.toString(),
                        "custom_blps_role" to user.role.name
                    )
                )
            )
        }

        user.erpCustomerId = customer.name
        return userRepository.save(user)
    }

    private fun ensureQuotation(extraService: ExtraServiceRequest) {
        if (!extraService.erpQuotationId.isNullOrBlank()) {
            return
        }

        ensureCustomer(extraService.chat.host)
        val guestCustomer = ensureCustomer(extraService.chat.guest)

        val quotation = withConnection { connection ->
            connection.createQuotation(
                ErpNextQuotationRequest(
                    company = requireCompany(),
                    partyName = guestCustomer.erpCustomerId!!,
                    transactionDate = extraService.createdAt.toLocalDate(),
                    items = listOf(
                        ErpNextQuotationItemRequest(
                            itemCode = requireServiceItemCode(),
                            qty = BigDecimal.ONE,
                            rate = extraService.amount,
                            description = buildServiceDescription(extraService)
                        )
                    ),
                    additionalFields = mapOf(
                        "custom_blps_extra_service_id" to extraService.id.toString(),
                        "custom_blps_chat_id" to extraService.chat.id.toString(),
                        "custom_blps_host_user_id" to extraService.chat.host.id.toString(),
                        "custom_blps_guest_user_id" to extraService.chat.guest.id.toString()
                    )
                )
            )
        }

        extraService.erpQuotationId = quotation.name
        extraServiceRequestRepository.save(extraService)
    }

    private fun ensureSalesOrder(extraService: ExtraServiceRequest) {
        if (!extraService.erpSalesOrderId.isNullOrBlank()) {
            return
        }
        ensureQuotation(extraService)
        val salesOrder = withConnection { connection ->
            connection.createSalesOrderFromQuotation(
                extraService.erpQuotationId!!,
                resolveDeliveryDate(extraService)
            )
        }
        extraService.erpSalesOrderId = salesOrder.name
        extraServiceRequestRepository.save(extraService)
    }

    private fun resolveDeliveryDate(extraService: ExtraServiceRequest): LocalDate =
        extraService.createdAt.toLocalDate().plusDays(1)

    private fun buildCustomerName(user: User): String {
        val fullName = listOf(user.firstName.trim(), user.lastName.trim())
            .filter { it.isNotBlank() }
            .joinToString(" ")
        return if (fullName.isBlank()) user.username else "$fullName (${user.username})"
    }

    private fun buildServiceDescription(extraService: ExtraServiceRequest): String =
        buildString {
            append(extraService.title)
            if (extraService.description.isNotBlank()) {
                append("\n\n")
                append(extraService.description)
            }
        }

    private fun loadUser(userId: UUID): User =
        userRepository.findById(userId)
            .orElseThrow { ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "user not found") }

    private fun loadExtraService(extraServiceId: UUID): ExtraServiceRequest =
        extraServiceRequestRepository.findById(extraServiceId)
            .orElseThrow { ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "extra service not found") }

    private fun requireCompany(): String =
        erpNextProperties.company.trim().ifBlank {
            throw ErpNextIntegrationException("ERPNext company is not configured")
        }

    private fun requireServiceItemCode(): String =
        erpNextProperties.serviceItemCode.trim().ifBlank {
            throw ErpNextIntegrationException("ERPNext service item code is not configured")
        }

    private fun <T> withConnection(block: (ErpNextJcaConnection) -> T): T {
        val connection = erpNextConnectionFactory.getConnection() as ErpNextJcaConnection
        try {
            return block(connection)
        } finally {
            connection.close()
        }
    }
}
