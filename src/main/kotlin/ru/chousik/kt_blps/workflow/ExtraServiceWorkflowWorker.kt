package ru.chousik.kt_blps.workflow

import java.util.UUID
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import ru.chousik.kt_blps.service.ErpSyncOutboxService
import ru.chousik.kt_blps.service.ExtraServiceRequestService

@Service
class ExtraServiceWorkflowWorker(
    private val camundaRestClient: CamundaRestClient,
    private val extraServiceRequestService: ExtraServiceRequestService,
    private val erpSyncOutboxService: ErpSyncOutboxService,
    @Value("\${app.camunda.worker-id:core-service}")
    private val workerId: String,
    @Value("\${app.camunda.external-task.max-tasks:10}")
    private val maxTasks: Int,
    @Value("\${app.camunda.external-task.lock-duration-ms:30000}")
    private val lockDurationMs: Long,
    @Value("\${app.camunda.external-task.failure-retries:3}")
    private val failureRetries: Int,
    @Value("\${app.camunda.external-task.failure-retry-timeout-ms:30000}")
    private val failureRetryTimeoutMs: Long
) {
    private val topics = listOf(
        "sync-extra-service-quotation",
        "request-extra-service-payment",
        "mark-extra-service-rejected",
        "mark-payment-link-assigned",
        "mark-payment-creation-failed",
        "sync-extra-service-sales-invoice"
    )

    @Scheduled(fixedDelayString = "\${app.camunda.external-task.fixed-delay-ms:3000}")
    fun processExternalTasks() {
        val tasks = camundaRestClient.fetchAndLock(workerId, topics, maxTasks, lockDurationMs)
        tasks.forEach { task ->
            try {
                handle(task)
                camundaRestClient.completeExternalTask(task.id, workerId)
            } catch (ex: Exception) {
                camundaRestClient.failExternalTask(
                    taskId = task.id,
                    workerId = workerId,
                    exception = ex,
                    retries = failureRetries,
                    retryTimeoutMs = failureRetryTimeoutMs
                )
            }
        }
    }

    private fun handle(task: ExternalTaskDto) {
        val extraServiceId = task.uuidVariable("extraServiceId")
        when (task.topicName) {
            "sync-extra-service-quotation" ->
                erpSyncOutboxService.enqueueSyncQuotationForExtraService(extraServiceId)

            "request-extra-service-payment" ->
                extraServiceRequestService.requestPaymentCreationFromWorkflow(
                    serviceId = extraServiceId,
                    initiatedByUserId = task.uuidVariable("initiatedByUserId")
                )

            "mark-extra-service-rejected" ->
                extraServiceRequestService.rejectExtraServiceFromWorkflow(extraServiceId)

            "mark-payment-link-assigned" ->
                extraServiceRequestService.markPaymentLinkAssigned(extraServiceId)

            "mark-payment-creation-failed" ->
                extraServiceRequestService.markPaymentCreationFailed(
                    serviceId = extraServiceId,
                    errorMessage = task.stringVariable("errorMessage").ifBlank { null }
                )

            "sync-extra-service-sales-invoice" ->
                erpSyncOutboxService.enqueueSyncSalesInvoiceForExtraService(extraServiceId)

            else -> error("Unsupported Camunda external task topic: ${task.topicName}")
        }
    }

    private fun ExternalTaskDto.uuidVariable(name: String): UUID =
        UUID.fromString(stringVariable(name))

    private fun ExternalTaskDto.stringVariable(name: String): String {
        val variable = variables[name] ?: error("Camunda variable '$name' is missing")
        return variable["value"]?.asText() ?: error("Camunda variable '$name' has no value")
    }
}
