package ru.chousik.blps_kt.model

enum class ExtraServiceRequestStatus {
    WAITING_GUEST_APPROVAL,
    PAYMENT_LINK_SENT,
    PAID,
    PAYMENT_FAILED,
    REJECTED,
    SERVICE_DELIVERED
}
