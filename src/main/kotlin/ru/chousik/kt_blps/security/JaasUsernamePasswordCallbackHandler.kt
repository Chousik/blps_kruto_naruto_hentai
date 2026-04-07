package ru.chousik.blps_kt.security

import javax.security.auth.callback.Callback
import javax.security.auth.callback.CallbackHandler
import javax.security.auth.callback.NameCallback
import javax.security.auth.callback.PasswordCallback
import javax.security.auth.callback.UnsupportedCallbackException

class JaasUsernamePasswordCallbackHandler(
    private val username: String,
    private val password: String
) : CallbackHandler {

    override fun handle(callbacks: Array<out Callback>) {
        callbacks.forEach { callback ->
            when (callback) {
                is NameCallback -> callback.name = username
                is PasswordCallback -> callback.password = password.toCharArray()
                else -> throw UnsupportedCallbackException(callback)
            }
        }
    }
}

