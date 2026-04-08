package ru.chousik.kt_blps.security

import javax.security.auth.Subject
import javax.security.auth.callback.CallbackHandler
import javax.security.auth.spi.LoginModule

class CustomLoginModule : LoginModule {
    override fun initialize(
        subject: Subject?,
        callbackHandler: CallbackHandler?,
        sharedState: Map<String?, *>?,
        options: Map<String?, *>?
    ) {
        TODO("Not yet implemented")
    }

    override fun login(): Boolean {
       return false;
    }

    override fun commit(): Boolean {
        TODO("Not yet implemented")
    }

    override fun abort(): Boolean {
        TODO("Not yet implemented")
    }

    override fun logout(): Boolean {
        TODO("Not yet implemented")
    }
}