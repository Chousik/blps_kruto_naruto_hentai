package ru.chousik.kt_blps.dto.erpnext

import java.time.LocalDate

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
