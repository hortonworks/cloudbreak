package com.sequenceiq.cloudbreak.cloud.handler

import javax.inject.Inject

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.cloud.CloudConnector
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext
import com.sequenceiq.cloudbreak.cloud.context.CloudContext
import com.sequenceiq.cloudbreak.cloud.event.resource.GetInstancesStateRequest
import com.sequenceiq.cloudbreak.cloud.event.resource.GetInstancesStateResult
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus

import reactor.bus.Event
import reactor.bus.EventBus

@Component
class InstanceStateHandler : CloudPlatformEventHandler<GetInstancesStateRequest<Any>> {

    @Inject
    private val cloudPlatformConnectors: CloudPlatformConnectors? = null
    @Inject
    private val eventBus: EventBus? = null

    override fun type(): Class<GetInstancesStateRequest<Any>> {
        return GetInstancesStateRequest<Any>::class.java
    }

    override fun accept(event: Event<GetInstancesStateRequest<Any>>) {
        LOGGER.info("Received event: {}", event)
        val request = event.data
        val cloudContext = request.cloudContext
        val result: GetInstancesStateResult
        try {
            val connector = cloudPlatformConnectors!!.get(cloudContext.platformVariant)
            val auth = connector.authentication().authenticate(cloudContext, request.cloudCredential)
            val instances = request.instances
            val instanceStatuses = connector.instances().check(auth, instances)
            result = GetInstancesStateResult(request, instanceStatuses)
        } catch (e: Exception) {
            result = GetInstancesStateResult("Instance state synchronizing failed", e, request)
        }

        request.result.onNext(result)
        eventBus!!.notify(result.selector(), Event(event.headers, result))
    }

    companion object {

        private val LOGGER = LoggerFactory.getLogger(InstanceStateHandler::class.java)
    }

}
