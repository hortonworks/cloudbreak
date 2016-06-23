package com.sequenceiq.cloudbreak.cloud.handler

import javax.inject.Inject

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.cloud.context.CloudContext
import com.sequenceiq.cloudbreak.cloud.event.setup.SshUserRequest
import com.sequenceiq.cloudbreak.cloud.event.setup.SshUserResponse
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential

import reactor.bus.Event

@Component
class SshUserHandler : CloudPlatformEventHandler<SshUserRequest<Any>> {

    @Inject
    private val cloudPlatformConnectors: CloudPlatformConnectors? = null

    override fun type(): Class<SshUserRequest<Any>> {
        return SshUserRequest<Any>::class.java
    }

    override fun accept(event: Event<SshUserRequest<Any>>) {
        LOGGER.info("Received event: {}", event)
        val request = event.data
        val cloudContext = request.cloudContext
        val cloudCredential = request.cloudCredential
        request.result.onNext(SshUserResponse(cloudContext, cloudCredential.loginUserName))
    }

    companion object {

        private val LOGGER = LoggerFactory.getLogger(SshUserHandler::class.java)
    }
}
