package ru.chousik.kt_blps

import jakarta.persistence.EntityManagerFactory
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.jta.JtaTransactionManager

@SpringBootTest
class BlpsKt2ApplicationTests {

    @Autowired
    private lateinit var entityManagerFactory: EntityManagerFactory

    @Autowired
    private lateinit var transactionManager: JtaTransactionManager

    @Test
    fun contextLoads() {
        assertThat(transactionManager.userTransaction).isNotNull
        assertThat(transactionManager.transactionManager).isNotNull
        assertThat(entityManagerFactory.properties["hibernate.transaction.jta.platform"])
            .isNotNull
            .matches { it!!::class.java.name == "org.springframework.boot.hibernate.SpringJtaPlatform" }
    }

}
