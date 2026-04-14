package ru.chousik.kt_blps.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.jta.JtaTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import com.arjuna.ats.jta.UserTransaction as NarayanaUserTransaction

@Configuration
class JtaNarayanaConfig {
    @Bean
    fun transactionManager(): PlatformTransactionManager =
        JtaTransactionManager(
            NarayanaUserTransaction.userTransaction()
        )

    @Bean("writeTransactionTemplate")
    fun writeTransactionTemplate(transactionManager: PlatformTransactionManager): TransactionTemplate =
        TransactionTemplate(transactionManager)

    @Bean("readOnlyTransactionTemplate")
    fun readOnlyTransactionTemplate(transactionManager: PlatformTransactionManager): TransactionTemplate =
        TransactionTemplate(transactionManager).apply {
            isReadOnly = true
        }
}
