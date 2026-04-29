package ru.chousik.kt_blps.repository

import java.util.UUID
import org.springframework.data.jpa.repository.JpaRepository
import ru.chousik.kt_blps.model.PaymentLinkReminderType
import ru.chousik.kt_blps.model.PaymentRequestReminder

interface PaymentRequestReminderRepository : JpaRepository<PaymentRequestReminder, UUID> {
    fun existsByPaymentRequestIdAndType(paymentRequestId: UUID, type: PaymentLinkReminderType): Boolean
}
