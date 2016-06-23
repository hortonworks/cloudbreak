package com.sequenceiq.cloudbreak.reactor

import javax.inject.Inject

import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.domain.Stack
import com.sequenceiq.cloudbreak.reactor.api.event.resource.ClusterCredentialChangeRequest
import com.sequenceiq.cloudbreak.reactor.api.event.resource.ClusterCredentialChangeResult
import com.sequenceiq.cloudbreak.service.cluster.flow.AmbariClusterConnector
import com.sequenceiq.cloudbreak.service.stack.StackService

import reactor.bus.Event
import reactor.bus.EventBus

@Component
class ClusterCredentialChangeHandler : ClusterEventHandler<ClusterCredentialChangeRequest> {
    @Inject
    private val ambariClusterConnector: AmbariClusterConnector? = null
    @Inject
    private val stackService: StackService? = null
    @Inject
    private val eventBus: EventBus? = null

    override fun type(): Class<ClusterCredentialChangeRequest> {
        return ClusterCredentialChangeRequest::class.java
    }

    override fun accept(event: Event<ClusterCredentialChangeRequest>) {
        val request = event.data
        val result: ClusterCredentialChangeResult
        try {
            val stack = stackService!!.getById(request.stackId)
            ambariClusterConnector!!.credentialChangeAmbariCluster(stack.id, request.user, request.password)
            result = ClusterCredentialChangeResult(request)
        } catch (e: Exception) {
            result = ClusterCredentialChangeResult(e.message, e, request)
        }

        eventBus!!.notify(result.selector(), Event(event.headers, result))
    }
}
