package ru.chousik.kt_blps.dto.erpnext

data class ErpNextDocument(
    val doctype: String,
    val name: String,
    val docstatus: Int? = null,
    val payload: Map<String, Any?> = emptyMap()
)
