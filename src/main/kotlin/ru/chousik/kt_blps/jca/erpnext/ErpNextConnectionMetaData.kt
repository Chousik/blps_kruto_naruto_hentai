package ru.chousik.kt_blps.jca.erpnext

import jakarta.resource.ResourceException
import jakarta.resource.cci.ConnectionMetaData

class ErpNextConnectionMetaData(
    private val baseUrl: String
) : ConnectionMetaData {
    override fun getEISProductName(): String = "ERPNext"

    override fun getEISProductVersion(): String = "unknown"

    override fun getUserName(): String = "token@${safeHost()}"

    private fun safeHost(): String =
        try {
            java.net.URI(baseUrl).host ?: "erpnext"
        } catch (_: Exception) {
            "erpnext"
        }
}
