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
import ru.chousik.kt_blps.dto.erpnext.ErpNextPaymentEntryRequest
import ru.chousik.kt_blps.dto.erpnext.ErpNextQuotationItemRequest
import ru.chousik.kt_blps.dto.erpnext.ErpNextQuotationRequest
import ru.chousik.kt_blps.model.ExtraServiceRequest
import ru.chousik.kt_blps.model.PaymentRequest
import ru.chousik.kt_blps.model.PaymentRequestStatus
import ru.chousik.kt_blps.model.User
import ru.chousik.kt_blps.repository.ExtraServiceRequestRepository
import ru.chousik.kt_blps.repository.PaymentRequestRepository
import ru.chousik.kt_blps.repository.UserRepository

@Service
class ErpNextSyncService(
    private val erpNextClient: ErpNextClient,
    private val erpNextProperties: ErpNextProperties,
    private val userRepository: UserRepository,
    private val extraServiceRequestRepository: ExtraServiceRequestRepository,
    private val paymentRequestRepository: PaymentRequestRepository,
    @Qualifier("writeTransactionTemplate")
    private val writeTransactionTemplate: TransactionTemplate
) {

    fun syncQuotationForExtraService(extraServiceId: UUID) {
        writeTransactionTemplate.executeWithoutResult {
            val extraService = loadExtraService(extraServiceId)
            if (!extraService.erpQuotationId.isNullOrBlank()) {
                return@executeWithoutResult
            }

            val customer = ensureCustomer(extraService.chat.guest)
            val quotation = erpNextClient.createQuotation(
                ErpNextQuotationRequest(
                    company = requireCompany(),
                    partyName = customer.erpCustomerId!!,
                    transactionDate = extraService.createdAt.toLocalDate(),
                    items = listOf(
                        ErpNextQuotationItemRequest(
                            itemCode = requireServiceItemCode(),
                            qty = BigDecimal.ONE,
                            rate = extraService.amount,
                            description = buildServiceDescription(extraService)
                        )
                    )
                )
            )

            extraService.erpQuotationId = quotation.name
            extraServiceRequestRepository.save(extraService)
        }
    }

    fun syncSalesFlowForAcceptedExtraService(extraServiceId: UUID) {
        writeTransactionTemplate.executeWithoutResult {
            val extraService = loadExtraService(extraServiceId)
            ensureCustomer(extraService.chat.guest)
            ensureQuotation(extraService)

            if (extraService.erpSalesOrderId.isNullOrBlank()) {
                val salesOrder = erpNextClient.createSalesOrderFromQuotation(extraService.erpQuotationId!!)
                extraService.erpSalesOrderId = salesOrder.name
                extraServiceRequestRepository.save(extraService)
            }

            if (extraService.erpSalesInvoiceId.isNullOrBlank()) {
                val salesInvoice = erpNextClient.createSalesInvoiceFromSalesOrder(extraService.erpSalesOrderId!!)
                extraService.erpSalesInvoiceId = salesInvoice.name
                extraServiceRequestRepository.save(extraService)
            }
        }
    }

    fun syncPaymentEntryForPaymentRequest(paymentRequestId: UUID) {
        writeTransactionTemplate.executeWithoutResult {
            val paymentRequest = loadPaymentRequest(paymentRequestId)
            if (!paymentRequest.erpPaymentEntryId.isNullOrBlank()) {
                return@executeWithoutResult
            }
            if (paymentRequest.status != PaymentRequestStatus.PAID) {
                return@executeWithoutResult
            }

            val extraService = loadExtraService(paymentRequest.extraServiceRequestId)
            ensureCustomer(extraService.chat.guest)
            ensureQuotation(extraService)
            ensureSalesOrder(extraService)
            ensureSalesInvoice(extraService)

            val paymentEntry = erpNextClient.createPaymentEntry(
                ErpNextPaymentEntryRequest(
                    referenceName = extraService.erpSalesInvoiceId!!,
                    postingDate = paymentRequest.resolvedAt?.toLocalDate() ?: LocalDate.now(),
                    referenceDate = paymentRequest.resolvedAt?.toLocalDate() ?: LocalDate.now(),
                    referenceNo = paymentRequest.providerPaymentId ?: paymentRequest.id.toString(),
                    remarks = "Payment for extra service ${extraService.id}"
                )
            )

            paymentRequest.erpPaymentEntryId = paymentEntry.name
            paymentRequestRepository.save(paymentRequest)
        }
    }

    private fun ensureCustomer(user: User): User {
        if (!user.erpCustomerId.isNullOrBlank()) {
            return user
        }

        val customer = erpNextClient.ensureCustomer(
            ErpNextCustomerRequest(
                customerName = buildCustomerName(user),
                customerGroup = "Individual",
                territory = "All Territories"
            )
        )

        user.erpCustomerId = customer.name
        return userRepository.save(user)
    }

    private fun ensureQuotation(extraService: ExtraServiceRequest) {
        if (extraService.erpQuotationId.isNullOrBlank()) {
            val customer = ensureCustomer(extraService.chat.guest)
            val quotation = erpNextClient.createQuotation(
                ErpNextQuotationRequest(
                    company = requireCompany(),
                    partyName = customer.erpCustomerId!!,
                    transactionDate = extraService.createdAt.toLocalDate(),
                    items = listOf(
                        ErpNextQuotationItemRequest(
                            itemCode = requireServiceItemCode(),
                            qty = BigDecimal.ONE,
                            rate = extraService.amount,
                            description = buildServiceDescription(extraService)
                        )
                    )
                )
            )
            extraService.erpQuotationId = quotation.name
            extraServiceRequestRepository.save(extraService)
        }
    }

    private fun ensureSalesOrder(extraService: ExtraServiceRequest) {
        ensureQuotation(extraService)
        if (extraService.erpSalesOrderId.isNullOrBlank()) {
            val salesOrder = erpNextClient.createSalesOrderFromQuotation(extraService.erpQuotationId!!)
            extraService.erpSalesOrderId = salesOrder.name
            extraServiceRequestRepository.save(extraService)
        }
    }

    private fun ensureSalesInvoice(extraService: ExtraServiceRequest) {
        ensureSalesOrder(extraService)
        if (extraService.erpSalesInvoiceId.isNullOrBlank()) {
            val salesInvoice = erpNextClient.createSalesInvoiceFromSalesOrder(extraService.erpSalesOrderId!!)
            extraService.erpSalesInvoiceId = salesInvoice.name
            extraServiceRequestRepository.save(extraService)
        }
    }

    private fun buildCustomerName(user: User): String {
        val fullName = listOf(user.firstName.trim(), user.lastName.trim())
            .filter { it.isNotBlank() }
            .joinToString(" ")
        return if (fullName.isBlank()) {
            user.username
        } else {
            "$fullName (${user.username})"
        }
    }

    private fun buildServiceDescription(extraService: ExtraServiceRequest): String =
        buildString {
            append(extraService.title)
            if (extraService.description.isNotBlank()) {
                append("\n\n")
                append(extraService.description)
            }
        }

    private fun loadExtraService(extraServiceId: UUID): ExtraServiceRequest =
        extraServiceRequestRepository.findById(extraServiceId)
            .orElseThrow { ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "extra service not found") }

    private fun loadPaymentRequest(paymentRequestId: UUID): PaymentRequest =
        paymentRequestRepository.findById(paymentRequestId)
            .orElseThrow { ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "payment request not found") }

    private fun requireCompany(): String =
        erpNextProperties.company.trim().ifBlank {
            throw ErpNextClientException("ERPNext company is not configured")
        }

    private fun requireServiceItemCode(): String =
        erpNextProperties.serviceItemCode.trim().ifBlank {
            throw ErpNextClientException("ERPNext service item code is not configured")
        }
}
