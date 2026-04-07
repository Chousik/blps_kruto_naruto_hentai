package ru.chousik.blps_kt.controller

import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import ru.chousik.blps_kt.api.security.CurrentUserResponse
import ru.chousik.blps_kt.security.CurrentAccountService

@RestController
@RequestMapping
class CurrentUserController(
    private val currentAccountService: CurrentAccountService
) {

    @GetMapping("/me")
    @PreAuthorize("hasAuthority('PRIV_CURRENT_USER_READ')")
    fun me(): CurrentUserResponse =
        CurrentUserResponse.from(currentAccountService.currentAccount())
}

