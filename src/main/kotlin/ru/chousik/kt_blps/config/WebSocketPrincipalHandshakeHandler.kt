package ru.chousik.blps_kt.config

import java.security.Principal
import org.springframework.http.server.ServletServerHttpRequest
import org.springframework.http.server.ServerHttpRequest
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.web.context.HttpSessionSecurityContextRepository
import org.springframework.stereotype.Component
import org.springframework.web.socket.WebSocketHandler
import org.springframework.web.socket.server.support.DefaultHandshakeHandler

@Component
class WebSocketPrincipalHandshakeHandler : DefaultHandshakeHandler() {

    override fun determineUser(
        request: ServerHttpRequest,
        wsHandler: WebSocketHandler,
        attributes: MutableMap<String, Any>
    ): Principal? {
        request.principal?.let { return it }

        if (request is ServletServerHttpRequest) {
            val session = request.servletRequest.getSession(false)
            val securityContext = session?.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY)
                as? SecurityContext
            val authentication = securityContext?.authentication
            if (authentication != null && authentication.isAuthenticated) {
                return authentication
            }
        }

        return super.determineUser(request, wsHandler, attributes)
    }
}
