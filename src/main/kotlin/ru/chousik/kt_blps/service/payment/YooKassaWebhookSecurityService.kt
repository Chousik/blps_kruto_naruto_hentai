package ru.chousik.blps_kt.service.payment

import java.math.BigInteger
import java.net.InetAddress
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import ru.chousik.blps_kt.config.YooKassaProperties

@Service
class YooKassaWebhookSecurityService(
    private val yooKassaProperties: YooKassaProperties
) {

    private val trustedCidrs = listOf(
        "185.71.76.0/27",
        "185.71.77.0/27",
        "77.75.153.0/25",
        "77.75.156.11/32",
        "77.75.156.35/32",
        "77.75.154.128/25",
        "2a02:5180::/32"
    )

    fun validateRemoteAddress(remoteAddress: String?) {
        if (!yooKassaProperties.webhookIpCheckEnabled) {
            return
        }

        val address = remoteAddress?.trim()
            ?: throw ResponseStatusException(HttpStatus.FORBIDDEN, "missing remote address")

        val inetAddress = try {
            InetAddress.getByName(address)
        } catch (_: Exception) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "invalid remote address")
        }

        if (inetAddress.isLoopbackAddress || inetAddress.isAnyLocalAddress) {
            return
        }

        if (trustedCidrs.none { matchesCidr(inetAddress, it) }) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "webhook source IP is not trusted")
        }
    }

    private fun matchesCidr(address: InetAddress, cidr: String): Boolean {
        val (baseAddressText, prefixLengthText) = cidr.split("/")
        val baseAddress = InetAddress.getByName(baseAddressText)
        if (baseAddress.javaClass != address.javaClass) {
            return false
        }

        val prefixLength = prefixLengthText.toInt()
        val addressBytes = address.address
        val baseBytes = baseAddress.address
        val bitLength = addressBytes.size * 8
        val shift = bitLength - prefixLength

        val addressValue = BigInteger(1, addressBytes).shiftRight(shift)
        val baseValue = BigInteger(1, baseBytes).shiftRight(shift)
        return addressValue == baseValue
    }
}
