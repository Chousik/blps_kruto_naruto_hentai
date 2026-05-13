package ru.chousik.kt_blps.model

enum class OutboxEventStatus {
    PENDING,
    SENT,
    FAILED,
    PUBLISHED,
    IN_PROGRESS
}
