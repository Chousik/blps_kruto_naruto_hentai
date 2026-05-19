package ru.chousik.kt_blps.workflow

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import java.net.HttpURLConnection
import java.net.URI
import java.nio.charset.StandardCharsets
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

@Component
class CamundaRestClient(
    private val objectMapper: ObjectMapper,
    @Value("\${app.camunda.base-url:http://localhost:8080/engine-rest}")
    private val baseUrl: String
) {
    private val client = RestClient.builder()
        .baseUrl(baseUrl.trimEnd('/'))
        .build()

    fun deployProcess(resourcePath: String, deploymentName: String) {
        val resource = ClassPathResource(resourcePath)
        val boundary = "----blps-camunda-${System.currentTimeMillis()}"
        val body = buildMultipartBody(boundary, deploymentName, resource.filename ?: "process.bpmn", resource.inputStream.readBytes())
        val connection = URI("${baseUrl.trimEnd('/')}/deployment/create").toURL().openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.doOutput = true
        connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
        connection.outputStream.use { it.write(body) }

        if (connection.responseCode !in 200..299) {
            val error = connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
            throw IllegalStateException("Camunda deployment failed with HTTP ${connection.responseCode}: $error")
        }
    }

    fun startProcess(processDefinitionKey: String, businessKey: String, variables: Map<String, Any?> = emptyMap()) {
        client.post()
            .uri("/process-definition/key/{key}/start", processDefinitionKey)
            .body(
                mapOf(
                    "businessKey" to businessKey,
                    "variables" to variables.toCamundaVariables()
                )
            )
            .retrieve()
            .toBodilessEntity()
    }

    fun correlateMessage(messageName: String, businessKey: String, variables: Map<String, Any?> = emptyMap()) {
        client.post()
            .uri("/message")
            .body(
                mapOf(
                    "messageName" to messageName,
                    "businessKey" to businessKey,
                    "processVariables" to variables.toCamundaVariables()
                )
            )
            .retrieve()
            .toBodilessEntity()
    }

    fun fetchAndLock(workerId: String, topics: Collection<String>, maxTasks: Int, lockDurationMs: Long): List<ExternalTaskDto> {
        val response = client.post()
            .uri("/external-task/fetchAndLock")
            .body(
                mapOf(
                    "workerId" to workerId,
                    "maxTasks" to maxTasks,
                    "usePriority" to true,
                    "topics" to topics.map {
                        mapOf(
                            "topicName" to it,
                            "lockDuration" to lockDurationMs,
                            "variables" to listOf("extraServiceId", "initiatedByUserId", "errorMessage")
                        )
                    }
                )
            )
            .retrieve()
            .body(String::class.java)
            ?: return emptyList()
        val root = objectMapper.readTree(response)

        return root.map { node ->
            ExternalTaskDto(
                id = node["id"].asText(),
                topicName = node["topicName"].asText(),
                variables = node["variables"] ?: objectMapper.createObjectNode()
            )
        }
    }

    fun completeExternalTask(taskId: String, workerId: String) {
        client.post()
            .uri("/external-task/{id}/complete", taskId)
            .body(mapOf("workerId" to workerId))
            .retrieve()
            .toBodilessEntity()
    }

    fun failExternalTask(
        taskId: String,
        workerId: String,
        exception: Exception,
        retries: Int,
        retryTimeoutMs: Long
    ) {
        client.post()
            .uri("/external-task/{id}/failure", taskId)
            .body(
                mapOf(
                    "workerId" to workerId,
                    "errorMessage" to (exception.message ?: exception.javaClass.simpleName).take(666),
                    "errorDetails" to exception.stackTraceToString().take(4000),
                    "retries" to retries,
                    "retryTimeout" to retryTimeoutMs
                )
            )
            .retrieve()
            .toBodilessEntity()
    }

    private fun Map<String, Any?>.toCamundaVariables(): Map<String, Map<String, Any?>> =
        mapValues { (_, value) ->
            mapOf(
                "value" to value,
                "type" to when (value) {
                    is Boolean -> "Boolean"
                    is Int, is Long -> "Long"
                    is Float, is Double -> "Double"
                    else -> "String"
                }
            )
        }

    private fun buildMultipartBody(boundary: String, deploymentName: String, filename: String, fileBytes: ByteArray): ByteArray {
        val lineBreak = "\r\n"
        val prefix = buildString {
            append("--").append(boundary).append(lineBreak)
            append("Content-Disposition: form-data; name=\"deployment-name\"").append(lineBreak).append(lineBreak)
            append(deploymentName).append(lineBreak)
            append("--").append(boundary).append(lineBreak)
            append("Content-Disposition: form-data; name=\"deploy-changed-only\"").append(lineBreak).append(lineBreak)
            append("true").append(lineBreak)
            append("--").append(boundary).append(lineBreak)
            append("Content-Disposition: form-data; name=\"data\"; filename=\"").append(filename).append("\"").append(lineBreak)
            append("Content-Type: text/xml").append(lineBreak).append(lineBreak)
        }.toByteArray(StandardCharsets.UTF_8)
        val suffix = "$lineBreak--$boundary--$lineBreak".toByteArray(StandardCharsets.UTF_8)
        return prefix + fileBytes + suffix
    }
}

data class ExternalTaskDto(
    val id: String,
    val topicName: String,
    val variables: JsonNode
)
