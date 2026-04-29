package ru.chousik.kt_blps.jca.erpnext

import jakarta.resource.NotSupportedException
import jakarta.resource.ResourceException
import jakarta.resource.cci.Connection
import jakarta.resource.cci.ConnectionMetaData
import jakarta.resource.cci.Interaction
import jakarta.resource.cci.LocalTransaction
import jakarta.resource.cci.ResultSetInfo
import java.time.LocalDate
import java.util.LinkedHashMap
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientResponseException
import ru.chousik.kt_blps.config.ErpNextProperties
import ru.chousik.kt_blps.dto.erpnext.ErpNextCustomerRequest
import ru.chousik.kt_blps.dto.erpnext.ErpNextDocument
import ru.chousik.kt_blps.dto.erpnext.ErpNextQuotationRequest
import tools.jackson.core.type.TypeReference
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper

class ErpNextJcaConnection(
    private val properties: ErpNextProperties,
    private val objectMapper: ObjectMapper
) : Connection {
    private var closed = false
    private val restClient = RestClient.builder()
        .baseUrl(properties.baseUrl.removeSuffix("/"))
        .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
        .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .defaultHeader(
            HttpHeaders.AUTHORIZATION,
            "token ${properties.apiKey}:${properties.apiSecret}"
        )
        .build()

    fun ensureCustomer(request: ErpNextCustomerRequest): ErpNextDocument {
        ensureOpen()
        findCustomerByName(request.customerName)?.let { return it }
        val payload = linkedMapOf<String, Any?>(
            "customer_name" to request.customerName,
            "customer_type" to request.customerType,
            "customer_group" to request.customerGroup,
            "territory" to request.territory
        )
        payload.putAll(request.additionalFields)
        return createResource("Customer", payload, submit = false)
    }

    fun createQuotation(request: ErpNextQuotationRequest): ErpNextDocument {
        ensureOpen()
        val payload = linkedMapOf<String, Any?>(
            "quotation_to" to request.quotationTo,
            "party_name" to request.partyName,
            "company" to request.company,
            "transaction_date" to request.transactionDate.toString(),
            "order_type" to request.orderType,
            "items" to request.items.map {
                linkedMapOf<String, Any?>(
                    "item_code" to it.itemCode,
                    "qty" to it.qty,
                    "rate" to it.rate
                ).apply {
                    it.description?.let { description -> put("description", description) }
                }
            }
        )
        request.validTill?.let { payload["valid_till"] = it.toString() }
        payload.putAll(request.additionalFields)
        return createResource("Quotation", payload, submit = true)
    }

    fun createSalesOrderFromQuotation(quotationName: String, deliveryDate: LocalDate): ErpNextDocument {
        ensureOpen()
        val draft = callMethod(
            "erpnext.selling.doctype.quotation.quotation.make_sales_order",
            linkedMapOf("source_name" to quotationName)
        )
        val payload = sanitizeDocumentForInsert(draft).toMutableMap()
        payload["delivery_date"] = deliveryDate.toString()
        val items = (payload["items"] as? List<*>)?.map { item ->
            val itemMap = item as? Map<*, *> ?: return@map item
            itemMap.entries.associate { (k, v) -> k.toString() to v }.toMutableMap().apply {
                this["delivery_date"] = deliveryDate.toString()
            }
        }
        if (items != null) payload["items"] = items
        return createResource("Sales Order", payload, submit = true)
    }

    fun createSalesInvoiceFromSalesOrder(salesOrderName: String): ErpNextDocument {
        ensureOpen()
        val draft = callMethod(
            "erpnext.selling.doctype.sales_order.sales_order.make_sales_invoice",
            linkedMapOf("source_name" to salesOrderName)
        )
        return createResource("Sales Invoice", sanitizeDocumentForInsert(draft), submit = true)
    }

    override fun createInteraction(): Interaction {
        throw NotSupportedException("CCI Interaction is not used by this adapter")
    }

    override fun getLocalTransaction(): LocalTransaction {
        throw NotSupportedException("Local transactions are managed by the application")
    }

    override fun getMetaData(): ConnectionMetaData = ErpNextConnectionMetaData(properties.baseUrl)

    override fun getResultSetInfo(): ResultSetInfo {
        throw NotSupportedException("ResultSetInfo is not supported by this adapter")
    }

    override fun close() {
        closed = true
    }

    private fun ensureOpen() {
        if (closed) {
            throw ResourceException("ERPNext JCA connection is closed")
        }
    }

    private fun findCustomerByName(customerName: String): ErpNextDocument? {
        val fields = objectMapper.writeValueAsString(listOf("name", "doctype", "docstatus"))
        val filters = objectMapper.writeValueAsString(listOf(listOf("customer_name", "=", customerName)))
        val response = execute("search Customer by customer_name") {
            restClient.get()
                .uri { builder ->
                    builder.path("/api/resource/Customer")
                        .queryParam("fields", fields)
                        .queryParam("filters", filters)
                        .queryParam("limit_page_length", 1)
                        .build()
                }
                .retrieve()
                .body(JsonNode::class.java)
                ?: error("Empty ERPNext response while searching Customer")
        }
        val data = requirePayload(response, "data")
        if (!data.isArray || data.isEmpty) return null
        return toDocument(data[0])
    }

    private fun createResource(doctype: String, payload: Map<String, Any?>, submit: Boolean): ErpNextDocument {
        val created = execute("create $doctype") {
            restClient.post()
                .uri("/api/resource/{doctype}", doctype)
                .body(payload)
                .retrieve()
                .body(JsonNode::class.java)
                ?: error("Empty ERPNext response while creating $doctype")
        }
        val createdDoc = requirePayload(created, "data")
        val name = createdDoc.path("name").asText()
        if (name.isBlank()) throw ErpNextIntegrationException("ERPNext create $doctype returned blank document name")
        if (!submit) return toDocument(createdDoc)
        submitResource(doctype, name)
        return getDocument(doctype, name)
    }

    private fun getDocument(doctype: String, name: String): ErpNextDocument {
        val node = execute("read $doctype/$name") {
            restClient.get()
                .uri("/api/resource/{doctype}/{name}", doctype, name)
                .retrieve()
                .body(JsonNode::class.java)
                ?: error("Empty ERPNext response while reading $doctype/$name")
        }
        return toDocument(requirePayload(node, "data"))
    }

    private fun submitResource(doctype: String, name: String) {
        execute("submit $doctype/$name") {
            restClient.post()
                .uri("/api/resource/{doctype}/{name}", doctype, name)
                .body(mapOf("run_method" to "submit"))
                .retrieve()
                .toBodilessEntity()
        }
    }

    private fun callMethod(methodPath: String, params: Map<String, Any?>): Map<String, Any?> {
        val response = execute("call $methodPath") {
            restClient.get()
                .uri { builder ->
                    val withPath = builder.path("/api/method/{methodPath}")
                    params.forEach { (key, value) -> value?.let { withPath.queryParam(key, it) } }
                    withPath.build(methodPath)
                }
                .retrieve()
                .body(JsonNode::class.java)
                ?: error("Empty ERPNext response while calling $methodPath")
        }
        return nodeToMap(requirePayload(response, "message"))
    }

    private fun sanitizeDocumentForInsert(document: Map<String, Any?>): Map<String, Any?> =
        when (val sanitized = sanitizeValue(document)) {
            is Map<*, *> -> sanitized.entries.associate { (key, value) -> key.toString() to value }
            else -> throw ErpNextIntegrationException("ERPNext mapped document has unexpected structure")
        }

    private fun sanitizeValue(value: Any?): Any? =
        when (value) {
            is Map<*, *> -> {
                val sanitized = LinkedHashMap<String, Any?>()
                value.forEach { (key, nestedValue) ->
                    val keyString = key?.toString() ?: return@forEach
                    if (keyString.startsWith("__") || keyString in INSERT_STRIP_KEYS) return@forEach
                    sanitized[keyString] = sanitizeValue(nestedValue)
                }
                sanitized
            }
            is List<*> -> value.map { sanitizeValue(it) }
            else -> value
        }

    private fun requirePayload(root: JsonNode, key: String): JsonNode {
        val payload = root.path(key)
        if (payload.isMissingNode || payload.isNull) {
            throw ErpNextIntegrationException("ERPNext response does not contain '$key'")
        }
        return payload
    }

    private fun toDocument(node: JsonNode): ErpNextDocument {
        val payload = nodeToMap(node)
        return ErpNextDocument(
            doctype = node.path("doctype").asText("").ifBlank { payload["doctype"]?.toString().orEmpty() },
            name = node.path("name").asText("").ifBlank { payload["name"]?.toString().orEmpty() },
            docstatus = node.path("docstatus").takeIf { !it.isMissingNode && !it.isNull }?.asInt(),
            payload = payload
        )
    }

    private fun nodeToMap(node: JsonNode): Map<String, Any?> =
        objectMapper.convertValue(node, object : TypeReference<Map<String, Any?>>() {})

    private fun <T> execute(operation: String, action: () -> T): T =
        try {
            action()
        } catch (ex: RestClientResponseException) {
            val message = buildString {
                append("ERPNext ")
                append(operation)
                append(" failed with HTTP ")
                append(ex.statusCode.value())
                val responseBody = ex.responseBodyAsString
                if (!responseBody.isNullOrBlank()) {
                    append(": ")
                    append(responseBody)
                }
            }
            throw ErpNextIntegrationException(message, ex)
        } catch (ex: Exception) {
            throw ErpNextIntegrationException("ERPNext $operation failed: ${ex.message}", ex)
        }

    companion object {
        private val INSERT_STRIP_KEYS = setOf(
            "name",
            "owner",
            "creation",
            "modified",
            "modified_by",
            "parent",
            "parenttype",
            "parentfield"
        )
    }
}
