package ru.chousik.kt_blps.config

import com.arjuna.ats.jta.TransactionManager as NarayanaTransactionManager
import com.arjuna.ats.jta.UserTransaction as NarayanaUserTransaction
import org.springframework.boot.hibernate.SpringJtaPlatform
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRED
import org.springframework.transaction.jta.JtaTransactionManager
import org.springframework.transaction.support.TransactionTemplate

@Configuration
class JtaNarayanaConfig {
    @Bean
    fun transactionManager(): JtaTransactionManager =
        JtaTransactionManager(
            NarayanaUserTransaction.userTransaction(),
            NarayanaTransactionManager.transactionManager()
        )

    @Bean("writeTransactionTemplate")
    fun writeTransactionTemplate(transactionManager: JtaTransactionManager): TransactionTemplate =
        TransactionTemplate(transactionManager)

    @Bean("readOnlyTransactionTemplate")
    fun readOnlyTransactionTemplate(transactionManager: JtaTransactionManager): TransactionTemplate =
        TransactionTemplate(transactionManager).apply {
            isReadOnly = true
        }
}
