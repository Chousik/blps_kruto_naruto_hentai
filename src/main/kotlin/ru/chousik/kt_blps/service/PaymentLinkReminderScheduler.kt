package ru.chousik.kt_blps.service

import java.time.Duration
import java.time.OffsetDateTime
import java.util.UUID
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate
import ru.chousik.kt_blps.model.Chat
import ru.chousik.kt_blps.model.PaymentLinkReminderType
import ru.chousik.kt_blps.model.PaymentRequestReminder
import ru.chousik.kt_blps.model.PaymentRequestStatus
import ru.chousik.kt_blps.repository.ChatRepository
import ru.chousik.kt_blps.repository.ExtraServiceRequestRepository
import ru.chousik.kt_blps.repository.PaymentRequestReminderRepository
import ru.chousik.kt_blps.repository.PaymentRequestRepository

@Service
class PaymentLinkReminderScheduler(
    private val paymentRequestRepository: PaymentRequestRepository,
    private val paymentRequestReminderRepository: PaymentRequestReminderRepository,
    private val extraServiceRequestRepository: ExtraServiceRequestRepository,
    private val chatRepository: ChatRepository,
    private val chatSystemMessageService: ChatSystemMessageService,
    @Qualifier("writeTransactionTemplate")
    private val writeTransactionTemplate: TransactionTemplate
) {

    @Scheduled(fixedDelayString = "\${app.payment-reminders.fixed-delay-ms:30000}")
    fun sendReminders() {
        writeTransactionTemplate.executeWithoutResult {
            val now = OffsetDateTime.now()
            val payments = paymentRequestRepository.findExpiringPayments(
                status = PaymentRequestStatus.PENDING,
                fromTime = now,
                toTime = now.plusMinutes(30)
            )

            for (payment in payments) {
                val expiresAt = payment.expiresAt ?: continue
                val extraService = extraServiceRequestRepository.findById(payment.extraServiceRequestId)
                    .orElse(null) ?: continue
                val remaining = Duration.between(now, expiresAt)

                when {
                    remaining <= Duration.ofMinutes(1) -> sendReminderIfNeeded(
                        paymentRequestId = payment.id,
                        type = PaymentLinkReminderType.ONE_MINUTE,
                        chat = extraService.chat,
                        message = "Less than 1 minute remains until the payment link for '${extraService.title}' expires.",
                        sentAt = now
                    )

                    remaining <= Duration.ofMinutes(10) -> sendReminderIfNeeded(
                        paymentRequestId = payment.id,
                        type = PaymentLinkReminderType.TEN_MINUTES,
                        chat = extraService.chat,
                        message = "Less than 10 minutes remain until the payment link for '${extraService.title}' expires.",
                        sentAt = now
                    )

                    remaining <= Duration.ofMinutes(30) -> sendReminderIfNeeded(
                        paymentRequestId = payment.id,
                        type = PaymentLinkReminderType.THIRTY_MINUTES,
                        chat = extraService.chat,
                        message = "Less than 30 minutes remain until the payment link for '${extraService.title}' expires.",
                        sentAt = now
                    )
                }
            }
        }
    }

    private fun sendReminderIfNeeded(
        paymentRequestId: UUID,
        type: PaymentLinkReminderType,
        chat: Chat,
        message: String,
        sentAt: OffsetDateTime
    ) {
        if (paymentRequestReminderRepository.existsByPaymentRequestIdAndType(paymentRequestId, type)) {
            return
        }

        chat.updatedAt = sentAt
        chatRepository.save(chat)
        chatSystemMessageService.append(chat, message)
        paymentRequestReminderRepository.save(
            PaymentRequestReminder().apply {
                this.paymentRequestId = paymentRequestId
                this.type = type
                this.sentAt = sentAt
            }
        )
    }
}
