package ru.chousik.blps_kt.security

import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.stereotype.Component
import ru.chousik.blps_kt.model.UserRole

@Component
class RolePrivilegeModel {

    private val rolePrivileges: Map<UserRole, Set<Privilege>> = mapOf(
        UserRole.GUEST to setOf(
            Privilege.PRIV_CURRENT_USER_READ,
            Privilege.PRIV_CHAT_CREATE,
            Privilege.PRIV_CHAT_READ,
            Privilege.PRIV_CHAT_LIST,
            Privilege.PRIV_CHAT_MESSAGE_READ,
            Privilege.PRIV_CHAT_MESSAGE_WRITE,
            Privilege.PRIV_EXTRA_SERVICE_READ,
            Privilege.PRIV_EXTRA_SERVICE_DECIDE,
            Privilege.PRIV_EXTRA_SERVICE_PAYMENT_READ
        ),
        UserRole.HOST to setOf(
            Privilege.PRIV_CURRENT_USER_READ,
            Privilege.PRIV_CHAT_CREATE,
            Privilege.PRIV_CHAT_READ,
            Privilege.PRIV_CHAT_LIST,
            Privilege.PRIV_CHAT_MESSAGE_READ,
            Privilege.PRIV_CHAT_MESSAGE_WRITE,
            Privilege.PRIV_EXTRA_SERVICE_CREATE,
            Privilege.PRIV_EXTRA_SERVICE_READ,
            Privilege.PRIV_EXTRA_SERVICE_UPDATE,
            Privilege.PRIV_EXTRA_SERVICE_DELETE,
            Privilege.PRIV_EXTRA_SERVICE_PAYMENT_READ
        ),
        UserRole.ADMIN to Privilege.entries.toSet()
    )

    fun privilegesFor(role: UserRole): Set<Privilege> =
        rolePrivileges[role] ?: emptySet()

    fun authoritiesFor(role: UserRole): List<GrantedAuthority> =
        buildList {
            add(SimpleGrantedAuthority("ROLE_${role.name}"))
            addAll(privilegesFor(role).map { SimpleGrantedAuthority(it.name) })
        }
}

