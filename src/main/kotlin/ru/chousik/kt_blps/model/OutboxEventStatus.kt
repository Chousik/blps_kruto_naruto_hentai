package ru.chousik.kt_blps.model

enum class OutboxEventStatus {
    PENDING,
    IN_PROGRESS,
    PUBLISHED
}
