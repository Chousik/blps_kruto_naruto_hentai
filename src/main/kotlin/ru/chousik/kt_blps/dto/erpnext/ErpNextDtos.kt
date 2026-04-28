package ru.chousik.kt_blps.dto.erpnext

import java.math.BigDecimal
import java.time.LocalDate

data class ErpNextDocument(
    val doctype: String,
    val name: String,
    val docstatus: Int? = null,
    val payload: Map<String, Any?> = emptyMap()
)

data class ErpNextCustomerRequest(
    val customerName: String,
    val customerType: String = "Individual",
    val customerGroup: String,
    val territory: String,
    val additionalFields: Map<String, Any?> = emptyMap()
)

data class ErpNextQuotationRequest(
    val company: String,
    val partyName: String,
    val transactionDate: LocalDate,
    val validTill: LocalDate? = null,
    val orderType: String = "Sales",
    val quotationTo: String = "Customer",
    val items: List<ErpNextQuotationItemRequest>,
    val additionalFields: Map<String, Any?> = emptyMap()
)

data class ErpNextQuotationItemRequest(
    val itemCode: String,
    val qty: BigDecimal,
    val rate: BigDecimal,
    val description: String? = null
)

data class ErpNextPaymentEntryRequest(
    val referenceName: String,
    val referenceDoctype: String = "Sales Invoice",
    val bankAccount: String? = null,
    val referenceNo: String? = null,
    val referenceDate: LocalDate? = null,
    val postingDate: LocalDate? = null,
    val remarks: String? = null,
    val additionalFields: Map<String, Any?> = emptyMap()
)
