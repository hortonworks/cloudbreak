package com.sequenceiq.periscope.service.registry

import org.slf4j.Logger
import org.slf4j.LoggerFactory

class RetryingServiceAddressResolver(private val serviceAddressResolver: ServiceAddressResolver, timeoutInMillis: Int) : ServiceAddressResolver {
    private var maxRetryCount: Int = 0

    init {
        maxRetryCount = timeoutInMillis / SLEEPTIME
        if (maxRetryCount <= 0) {
            maxRetryCount = 1
        }
    }

    @Throws(ServiceAddressResolvingException::class)
    override fun resolveUrl(serverUrl: String, protocol: String, serviceId: String): String {
        var attemptCount = 0
        var resolvedAddress: String? = null
        while (resolvedAddress == null && attemptCount < maxRetryCount) {
            try {
                resolvedAddress = serviceAddressResolver.resolveUrl(serverUrl, protocol, serviceId)
            } catch (e: ServiceAddressResolvingException) {
                handleException(e, attemptCount)
            }

            attemptCount++
        }
        return resolvedAddress
    }

    @Throws(ServiceAddressResolvingException::class)
    override fun resolveHostPort(host: String, port: String, serviceId: String): String {
        var attemptCount = 0
        var resolvedAddress: String? = null
        while (resolvedAddress == null && attemptCount < maxRetryCount) {
            try {
                resolvedAddress = serviceAddressResolver.resolveHostPort(host, port, serviceId)
            } catch (e: ServiceAddressResolvingException) {
                handleException(e, attemptCount)
            }

            attemptCount++
        }
        return resolvedAddress
    }

    @Throws(ServiceAddressResolvingException::class)
    private fun handleException(e: ServiceAddressResolvingException, attemptCount: Int) {
        if (attemptCount == maxRetryCount - 1) {
            throw e
        } else {
            try {
                LOGGER.warn("Unsuccessful address resolving: {}, retrying in {}millis", e.message, SLEEPTIME)
                Thread.sleep(SLEEPTIME.toLong())
            } catch (ie: InterruptedException) {
                LOGGER.warn("Interrupted exception occurred.", ie.message)
            }

        }
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(RetryingServiceAddressResolver::class.java)
        private val SLEEPTIME = 2000
    }
}
