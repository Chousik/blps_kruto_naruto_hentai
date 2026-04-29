package ru.chousik.kt_blps.service

import java.time.OffsetDateTime
import java.util.UUID
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate
import ru.chousik.kt_blps.model.ErpSyncOutboxTask
import ru.chousik.kt_blps.model.ErpSyncOutboxTaskStatus
import ru.chousik.kt_blps.model.ErpSyncOutboxTaskType
import ru.chousik.kt_blps.repository.ErpSyncOutboxTaskRepository

@Service
class ErpSyncOutboxService(
    private val erpSyncOutboxTaskRepository: ErpSyncOutboxTaskRepository,
    private val erpNextSyncService: ErpNextSyncService,
    @Qualifier("writeTransactionTemplate")
    private val writeTransactionTemplate: TransactionTemplate
) {

    fun enqueueSyncCustomerForUser(userId: UUID) {
        enqueue(ErpSyncOutboxTaskType.SYNC_USER_CUSTOMER, userId)
    }

    fun enqueueSyncQuotationForExtraService(extraServiceId: UUID) {
        enqueue(ErpSyncOutboxTaskType.SYNC_EXTRA_SERVICE_QUOTATION, extraServiceId)
    }

    fun enqueueSyncSalesOrderForExtraService(extraServiceId: UUID) {
        enqueue(ErpSyncOutboxTaskType.SYNC_EXTRA_SERVICE_SALES_ORDER, extraServiceId)
    }

    fun enqueueSyncSalesInvoiceForExtraService(extraServiceId: UUID) {
        enqueue(ErpSyncOutboxTaskType.SYNC_EXTRA_SERVICE_SALES_INVOICE, extraServiceId)
    }

    fun processReadyTasks(batchSize: Int, maxAttempts: Int) {
        val now = OffsetDateTime.now()
        val tasks = writeTransactionTemplate.execute {
            erpSyncOutboxTaskRepository.findAllByStatusAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
                status = ErpSyncOutboxTaskStatus.PENDING,
                nextAttemptAt = now,
                pageable = PageRequest.of(0, batchSize)
            )
        }.orEmpty()

        for (task in tasks) {
            processTask(task.id, maxAttempts)
        }
    }

    private fun enqueue(taskType: ErpSyncOutboxTaskType, aggregateId: UUID) {
        val now = OffsetDateTime.now()
        val existing = erpSyncOutboxTaskRepository.findByTaskTypeAndAggregateId(taskType, aggregateId)
        if (existing != null) {
            existing.status = ErpSyncOutboxTaskStatus.PENDING
            existing.attemptCount = 0
            existing.nextAttemptAt = now
            existing.lastError = null
            existing.updatedAt = now
            erpSyncOutboxTaskRepository.save(existing)
            return
        }

        erpSyncOutboxTaskRepository.save(
            ErpSyncOutboxTask().apply {
                this.taskType = taskType
                this.aggregateId = aggregateId
                status = ErpSyncOutboxTaskStatus.PENDING
                attemptCount = 0
                nextAttemptAt = now
                createdAt = now
                updatedAt = now
            }
        )
    }

    private fun processTask(taskId: UUID, maxAttempts: Int) {
        val task = writeTransactionTemplate.execute {
            erpSyncOutboxTaskRepository.findById(taskId).orElse(null)
        } ?: return

        if (task.status != ErpSyncOutboxTaskStatus.PENDING || task.nextAttemptAt.isAfter(OffsetDateTime.now())) {
            return
        }

        try {
            dispatch(task)
            markDone(task.id)
        } catch (ex: Exception) {
            markFailure(task.id, task.attemptCount + 1, ex, maxAttempts)
        }
    }

    private fun dispatch(task: ErpSyncOutboxTask) {
        when (task.taskType) {
            ErpSyncOutboxTaskType.SYNC_USER_CUSTOMER ->
                erpNextSyncService.syncCustomerForUser(task.aggregateId)

            ErpSyncOutboxTaskType.SYNC_EXTRA_SERVICE_QUOTATION ->
                erpNextSyncService.syncQuotationForExtraService(task.aggregateId)

            ErpSyncOutboxTaskType.SYNC_EXTRA_SERVICE_SALES_ORDER ->
                erpNextSyncService.syncSalesOrderForAcceptedExtraService(task.aggregateId)

            ErpSyncOutboxTaskType.SYNC_EXTRA_SERVICE_SALES_INVOICE ->
                erpNextSyncService.syncSalesInvoiceForExtraService(task.aggregateId)
        }
    }

    private fun markDone(taskId: UUID) {
        writeTransactionTemplate.executeWithoutResult {
            val task = erpSyncOutboxTaskRepository.findById(taskId).orElse(null) ?: return@executeWithoutResult
            val now = OffsetDateTime.now()
            task.status = ErpSyncOutboxTaskStatus.DONE
            task.lastError = null
            task.nextAttemptAt = now
            task.updatedAt = now
            erpSyncOutboxTaskRepository.save(task)
        }
    }

    private fun markFailure(taskId: UUID, nextAttemptCount: Int, exception: Exception, maxAttempts: Int) {
        writeTransactionTemplate.executeWithoutResult {
            val task = erpSyncOutboxTaskRepository.findById(taskId).orElse(null) ?: return@executeWithoutResult
            val now = OffsetDateTime.now()
            task.attemptCount = nextAttemptCount
            task.status = if (nextAttemptCount >= maxAttempts) {
                ErpSyncOutboxTaskStatus.FAILED
            } else {
                ErpSyncOutboxTaskStatus.PENDING
            }
            task.nextAttemptAt = now.plus(computeBackoff(nextAttemptCount))
            task.lastError = exception.stackTraceToString().take(4000)
            task.updatedAt = now
            erpSyncOutboxTaskRepository.save(task)
        }
    }

    private fun computeBackoff(attemptCount: Int) = when (attemptCount) {
        1 -> java.time.Duration.ofMinutes(1)
        2 -> java.time.Duration.ofMinutes(5)
        3 -> java.time.Duration.ofMinutes(15)
        4 -> java.time.Duration.ofHours(1)
        else -> java.time.Duration.ofHours(6)
    }
}
