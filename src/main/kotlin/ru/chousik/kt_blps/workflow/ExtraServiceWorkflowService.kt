package ru.chousik.kt_blps.workflow

import jakarta.annotation.PostConstruct
import java.util.UUID
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class ExtraServiceWorkflowService(
    private val camundaRestClient: CamundaRestClient,
    @Value("\${app.camunda.deploy-on-startup:true}")
    private val deployOnStartup: Boolean,
    @Value("\${app.camunda.extra-service.guest-decision-timeout:PT24H}")
    private val guestDecisionTimeout: String
) {
    @PostConstruct
    fun deployProcess() {
        if (deployOnStartup) {
            camundaRestClient.deployProcess(
                resourcePath = "bpmn/extra-service-process.bpmn",
                deploymentName = "blps-extra-service-process"
            )
        }
    }

    fun startExtraServiceProcess(extraServiceId: UUID, chatId: UUID, hostUserId: UUID, guestUserId: UUID) {
        camundaRestClient.startProcess(
            processDefinitionKey = PROCESS_KEY,
            businessKey = extraServiceId.toString(),
            variables = mapOf(
                "extraServiceId" to extraServiceId.toString(),
                "chatId" to chatId.toString(),
                "hostUserId" to hostUserId.toString(),
                "guestUserId" to guestUserId.toString(),
                "guestDecisionTimeout" to guestDecisionTimeout
            )
        )
    }

    fun acceptExtraService(extraServiceId: UUID, initiatedByUserId: UUID) {
        camundaRestClient.completeUserTaskByBusinessKey(
            processBusinessKey = extraServiceId.toString(),
            taskDefinitionKey = TASK_USER_DECIDE_PAYMENT,
            variables = mapOf(
                "extraServiceId" to extraServiceId.toString(),
                "initiatedByUserId" to initiatedByUserId.toString(),
                "decision" to "ACCEPT"
            )
        )
    }

    fun rejectExtraService(extraServiceId: UUID, initiatedByUserId: UUID) {
        camundaRestClient.completeUserTaskByBusinessKey(
            processBusinessKey = extraServiceId.toString(),
            taskDefinitionKey = TASK_USER_DECIDE_PAYMENT,
            variables = mapOf(
                "extraServiceId" to extraServiceId.toString(),
                "initiatedByUserId" to initiatedByUserId.toString(),
                "decision" to "REJECT"
            )
        )
    }

    fun notifyPaymentLinkAssigned(extraServiceId: UUID, paymentRequestId: UUID) {
        camundaRestClient.correlateMessage(
            messageName = MESSAGE_PAYMENT_LINK_ASSIGNED,
            businessKey = extraServiceId.toString(),
            variables = mapOf(
                "extraServiceId" to extraServiceId.toString(),
                "paymentRequestId" to paymentRequestId.toString()
            )
        )
    }

    fun notifyPaymentCreationFailed(extraServiceId: UUID, errorMessage: String?) {
        camundaRestClient.correlateMessage(
            messageName = MESSAGE_PAYMENT_CREATION_FAILED,
            businessKey = extraServiceId.toString(),
            variables = mapOf(
                "extraServiceId" to extraServiceId.toString(),
                "errorMessage" to errorMessage.orEmpty()
            )
        )
    }

    companion object {
        const val PROCESS_KEY = "extra-service-process"
        const val TASK_USER_DECIDE_PAYMENT = "Task_user_decide_payment"
        const val MESSAGE_PAYMENT_LINK_ASSIGNED = "PaymentLinkAssigned"
        const val MESSAGE_PAYMENT_CREATION_FAILED = "PaymentCreationFailed"
    }
}
