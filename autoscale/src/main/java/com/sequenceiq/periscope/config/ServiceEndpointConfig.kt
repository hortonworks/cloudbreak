package com.sequenceiq.periscope.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

import com.sequenceiq.periscope.service.registry.DNSServiceAddressResolver
import com.sequenceiq.periscope.service.registry.RetryingServiceAddressResolver
import com.sequenceiq.periscope.service.registry.ServiceAddressResolver
import com.sequenceiq.periscope.service.registry.ServiceAddressResolvingException

@Configuration
class ServiceEndpointConfig {
    @Value("${periscope.address.resolving.timeout:60000}")
    private val resolvingTimeout: Int = 0

    @Value("${periscope.db.tcp.addr:}")
    private val dbHost: String? = null

    @Value("${periscope.db.tcp.port:}")
    private val dbPort: String? = null

    @Value("${periscope.db.serviceid:}")
    private val databaseId: String? = null

    @Value("${periscope.identity.server.url:}")
    private val identityServiceUrl: String? = null

    @Value("${periscope.identity.serviceid:}")
    private val identityServiceId: String? = null

    @Value("${periscope.cloudbreak.url:}")
    private val cloudbreakUrl: String? = null

    @Value("${periscope.cloudbreak.serviceid:}")
    private val cloudbreakServiceId: String? = null

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

    @Bean
    @Throws(ServiceAddressResolvingException::class)
    fun cloudbreakUrl(): String {
        return serviceAddressResolver().resolveUrl(cloudbreakUrl, "http", cloudbreakServiceId)
    }
}
