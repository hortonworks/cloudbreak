package com.sequenceiq.cloudbreak.service.registry

interface ServiceAddressResolver {
    @Throws(ServiceAddressResolvingException::class)
    fun resolveUrl(serverUrl: String, protocol: String, serviceId: String): String

    @Throws(ServiceAddressResolvingException::class)
    fun resolveHostPort(host: String, port: String, serviceId: String): String
}
