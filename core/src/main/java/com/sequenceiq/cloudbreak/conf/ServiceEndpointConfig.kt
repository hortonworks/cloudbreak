package com.sequenceiq.cloudbreak.conf

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

import com.sequenceiq.cloudbreak.service.registry.DNSServiceAddressResolver
import com.sequenceiq.cloudbreak.service.registry.RetryingServiceAddressResolver
import com.sequenceiq.cloudbreak.service.registry.ServiceAddressResolver
import com.sequenceiq.cloudbreak.service.registry.ServiceAddressResolvingException

@Configuration
class ServiceEndpointConfig {
    @Value("${cb.address.resolving.timeout:}")
    private val resolvingTimeout: Int = 0

    @Value("${cb.db.port.5432.tcp.addr:}")
    private val dbHost: String? = null

    @Value("${cb.db.port.5432.tcp.port:}")
    private val dbPort: String? = null

    @Value("${cb.db.serviceid:}")
    private val databaseId: String? = null

    @Value("${cb.identity.server.url:}")
    private val identityServiceUrl: String? = null

    @Value("${cb.identity.serviceid:}")
    private val identityServiceId: String? = null

    @Bean
    fun serviceAddressResolver(): ServiceAddressResolver {
        return RetryingServiceAddressResolver(DNSServiceAddressResolver(), resolvingTimeout)
    }

    @Bean
    @Throws(ServiceAddressResolvingException::class)
    fun databaseAddress(): String {
        return serviceAddressResolver().resolveHostPort(dbHost, dbPort, databaseId)
    }

    @Bean
    @Throws(ServiceAddressResolvingException::class)
    fun identityServerUrl(): String {
        return serviceAddressResolver().resolveUrl(identityServiceUrl, "http", identityServiceId)
    }
}
