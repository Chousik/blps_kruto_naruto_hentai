package ru.chousik.kt_blps.jca.erpnext

import jakarta.resource.ResourceException
import jakarta.resource.cci.ConnectionFactory
import jakarta.resource.cci.MappedRecord
import java.time.LocalDate
import org.springframework.stereotype.Service
import ru.chousik.kt_blps.dto.erpnext.ErpNextCustomerRequest
import ru.chousik.kt_blps.dto.erpnext.ErpNextDocument
import ru.chousik.kt_blps.dto.erpnext.ErpNextQuotationRequest

@Service
class ErpNextJcaClient(
    private val connectionFactory: ConnectionFactory
) {
    fun ensureCustomer(request: ErpNextCustomerRequest): ErpNextDocument =
        execute(
            "ensureCustomer",
            linkedMapOf<String, Any>(
                "customerName" to request.customerName,
                "customerType" to request.customerType,
                "customerGroup" to request.customerGroup,
                "territory" to request.territory,
                "additionalFields" to LinkedHashMap(request.additionalFields)
            )
        )

    fun createQuotation(request: ErpNextQuotationRequest): ErpNextDocument =
        linkedMapOf<String, Any>(
            "company" to request.company,
            "partyName" to request.partyName,
            "transactionDate" to request.transactionDate.toString(),
            "orderType" to request.orderType,
            "quotationTo" to request.quotationTo,
            "items" to request.items.map { item ->
                linkedMapOf<String, Any>(
                    "itemCode" to item.itemCode,
                    "qty" to item.qty,
                    "rate" to item.rate
                ).apply {
                    item.description?.takeIf { it.isNotBlank() }?.let { put("description", it) }
                }
            },
            "additionalFields" to LinkedHashMap(request.additionalFields)
        ).apply {
            request.validTill?.let { put("validTill", it.toString()) }
        }.let { execute("createQuotation", it) }

    fun createSalesOrderFromQuotation(quotationName: String, deliveryDate: LocalDate): ErpNextDocument =
        execute(
            "createSalesOrderFromQuotation",
            linkedMapOf<String, Any>(
                "quotationName" to quotationName,
                "deliveryDate" to deliveryDate.toString()
            )
        )

    fun createSalesInvoiceFromSalesOrder(salesOrderName: String): ErpNextDocument =
        execute(
            "createSalesInvoiceFromSalesOrder",
            linkedMapOf<String, Any>("salesOrderName" to salesOrderName)
        )

    private fun execute(operation: String, attributes: Map<String, Any>): ErpNextDocument {
        val input = connectionFactory.getRecordFactory().createMappedRecord<String, Any>("erpnextRequest")
        input["operation"] = operation
        input.putAll(attributes)

        val connection = connectionFactory.getConnection()
        try {
            val interaction = connection.createInteraction()
            try {
                val output = interaction.execute(null, input) as? MappedRecord<*, *>
                    ?: throw ResourceException("ERPNext interaction did not return a MappedRecord")
                return toDocument(output)
            } finally {
                interaction.close()
            }
        } finally {
            connection.close()
        }
    }

    private fun toDocument(record: MappedRecord<*, *>): ErpNextDocument =
        ErpNextDocument(
            doctype = record["doctype"]?.toString().orEmpty(),
            name = record["name"]?.toString().orEmpty(),
            docstatus = when (val value = record["docstatus"]) {
                is Number -> value.toInt()
                is String -> value.toIntOrNull()
                else -> null
            },
            payload = when (val payload = record["payload"]) {
                is Map<*, *> -> payload.entries.associate { (key, value) -> key.toString() to value }
                else -> emptyMap()
            }
        )
}
