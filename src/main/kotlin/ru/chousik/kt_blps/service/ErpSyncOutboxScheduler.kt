package ru.chousik.kt_blps.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class ErpSyncOutboxScheduler(
    private val erpSyncOutboxService: ErpSyncOutboxService,
    @Value("\${app.erp-sync-outbox.batch-size:20}")
    private val batchSize: Int,
    @Value("\${app.erp-sync-outbox.max-attempts:20}")
    private val maxAttempts: Int
) {

    @Scheduled(fixedDelayString = "\${app.erp-sync-outbox.fixed-delay-ms:30000}")
    fun processPendingTasks() {
        erpSyncOutboxService.processReadyTasks(batchSize, maxAttempts)
    }
}
