package ru.chousik.kt_blps.jca.erpnext

import jakarta.resource.NotSupportedException
import jakarta.resource.ResourceException
import jakarta.resource.cci.Connection
import jakarta.resource.cci.ConnectionFactory
import jakarta.resource.cci.ConnectionSpec
import jakarta.resource.cci.RecordFactory
import jakarta.resource.cci.ResourceAdapterMetaData
import javax.naming.Reference
import ru.chousik.kt_blps.config.ErpNextProperties
import tools.jackson.databind.ObjectMapper

class ErpNextJcaConnectionFactory(
    private val properties: ErpNextProperties,
    private val objectMapper: ObjectMapper
) : ConnectionFactory {
    @Volatile
    private var reference: Reference? = null

    override fun getConnection(): Connection = ErpNextJcaConnection(properties, objectMapper)

    override fun getConnection(properties: ConnectionSpec): Connection = getConnection()

    override fun getRecordFactory(): RecordFactory {
        throw NotSupportedException("RecordFactory is not supported by this adapter")
    }

    override fun getMetaData(): ResourceAdapterMetaData = object : ResourceAdapterMetaData {
        override fun getAdapterVersion(): String = "1.0.0"
        override fun getAdapterVendorName(): String = "ru.chousik"
        override fun getAdapterName(): String = "ERPNext JCA Adapter"
        override fun getAdapterShortDescription(): String = "JCA outbound adapter for ERPNext HTTP API"
        override fun getSpecVersion(): String = "2.1"
        override fun getInteractionSpecsSupported(): Array<String> = emptyArray()
        override fun supportsExecuteWithInputAndOutputRecord(): Boolean = false
        override fun supportsExecuteWithInputRecordOnly(): Boolean = false
        override fun supportsLocalTransactionDemarcation(): Boolean = false
    }

    override fun setReference(reference: Reference) {
        this.reference = reference
    }

    override fun getReference(): Reference? = reference
}
