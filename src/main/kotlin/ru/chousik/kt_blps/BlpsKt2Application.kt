package ru.chousik.kt_blps

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class BlpsKt2Application

fun main(args: Array<String>) {
    runApplication<BlpsKt2Application>(*args)
}
