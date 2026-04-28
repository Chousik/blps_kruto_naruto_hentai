package ru.chousik.payment_worker_service

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class PaymentWorkerServiceApplication

fun main(args: Array<String>) {
    runApplication<PaymentWorkerServiceApplication>(*args)
}
