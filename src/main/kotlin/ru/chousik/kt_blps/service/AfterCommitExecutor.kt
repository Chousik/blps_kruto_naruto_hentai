package ru.chousik.kt_blps.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager

@Component
class AfterCommitExecutor {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun run(description: String, action: () -> Unit) {
        val safeAction = {
            try {
                action()
            } catch (ex: Exception) {
                logger.error("After-commit action failed: {}", description, ex)
            }
        }

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(object : TransactionSynchronization {
                override fun afterCommit() {
                    safeAction()
                }
            })
        } else {
            safeAction()
        }
    }
}
