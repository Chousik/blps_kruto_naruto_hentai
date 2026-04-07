package ru.chousik.blps_kt.config

import com.arjuna.ats.jta.TransactionManager
import com.arjuna.ats.jta.UserTransaction
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.jta.JtaTransactionManager
import org.springframework.transaction.support.TransactionTemplate

@Configuration
class JtaNarayanaConfig {

    @Bean(name = ["jtaTransactionManager", "transactionManager"])
    fun jtaTransactionManager(): JtaTransactionManager =
        JtaTransactionManager(
            UserTransaction.userTransaction(),
            TransactionManager.transactionManager()
        )

    @Bean(name = ["jtaTransactionTemplate"])
    fun jtaTransactionTemplate(
        transactionManager: PlatformTransactionManager
    ): TransactionTemplate = TransactionTemplate(transactionManager)

    @Bean(name = ["jtaRequiresNewTransactionTemplate"])
    fun jtaRequiresNewTransactionTemplate(
        transactionManager: PlatformTransactionManager
    ): TransactionTemplate =
        TransactionTemplate(transactionManager).apply {
            propagationBehavior = TransactionDefinition.PROPAGATION_REQUIRES_NEW
        }
}

