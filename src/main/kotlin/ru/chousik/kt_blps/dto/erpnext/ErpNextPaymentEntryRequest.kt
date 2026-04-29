package ru.chousik.kt_blps.dto.erpnext

import java.time.LocalDate

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
