package com.sequenceiq.periscope.service.configuration

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

import com.sequenceiq.cloudbreak.client.CloudbreakClient
import com.sequenceiq.cloudbreak.client.CloudbreakClient.CloudbreakClientBuilder


@Configuration
class CloudbreakClientConfiguration {

    @Autowired
    @Qualifier("cloudbreakUrl")
    private val cloudbreakUrl: String? = null

    @Value("${cb.server.contextPath:/cb}")
    private val cbRootContextPath: String? = null

    @Autowired
    @Qualifier("identityServerUrl")
    private val identityServerUrl: String? = null

    @Value("${periscope.client.id}")
    private val clientId: String? = null

    @Value("${periscope.client.secret}")
    private val secret: String? = null

    @Bean
    fun cloudbreakClient(): CloudbreakClient {
        return CloudbreakClientBuilder(cloudbreakUrl!! + cbRootContextPath!!, identityServerUrl, clientId).withSecret(secret).build()
    }


}
