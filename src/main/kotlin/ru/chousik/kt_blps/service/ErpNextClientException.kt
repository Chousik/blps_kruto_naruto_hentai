package ru.chousik.kt_blps.service

class ErpNextClientException(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)
