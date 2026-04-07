package ru.chousik.blps_kt.api.chat

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class CreateChatMessageRequest(
    @field:NotBlank
    @field:Size(max = 2000)
    val message: String?
)
