package ru.chousik.kt_blps.dto.erpnext

import java.math.BigDecimal

data class ErpNextQuotationItemRequest(
    val itemCode: String,
    val qty: BigDecimal,
    val rate: BigDecimal,
    val description: String? = null
)
