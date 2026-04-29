package ru.chousik.kt_blps.dto.erpnext

data class ErpNextCustomerRequest(
    val customerName: String,
    val customerType: String = "Individual",
    val customerGroup: String,
    val territory: String,
    val additionalFields: Map<String, Any?> = emptyMap()
)
