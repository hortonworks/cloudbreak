package com.sequenceiq.cloudbreak.cloud.handler

import javax.inject.Inject

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

import com.google.common.collect.Sets
import com.sequenceiq.cloudbreak.cloud.CloudConnector
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext
import com.sequenceiq.cloudbreak.cloud.context.CloudContext
import com.sequenceiq.cloudbreak.cloud.event.instance.InstancesStatusResult
import com.sequenceiq.cloudbreak.cloud.event.instance.StartInstancesRequest
import com.sequenceiq.cloudbreak.cloud.event.instance.StartInstancesResult
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus
import com.sequenceiq.cloudbreak.cloud.scheduler.SyncPollingScheduler
import com.sequenceiq.cloudbreak.cloud.task.PollTask
import com.sequenceiq.cloudbreak.cloud.task.PollTaskFactory

import reactor.bus.Event
import reactor.bus.EventBus

@Component
class StartStackHandler : CloudPlatformEventHandler<StartInstancesRequest> {

    @Inject
    private val cloudPlatformConnectors: CloudPlatformConnectors? = null
    @Inject
    private val statusCheckFactory: PollTaskFactory? = null
    @Inject
    private val syncPollingScheduler: SyncPollingScheduler<InstancesStatusResult>? = null
    @Inject
    private val eventBus: EventBus? = null

    override fun type(): Class<StartInstancesRequest> {
        return StartInstancesRequest::class.java
    }

    override fun accept(event: Event<StartInstancesRequest>) {
        LOGGER.info("Received event: {}", event)
        val request = event.data
        val cloudContext = request.cloudContext
        try {
            val connector = cloudPlatformConnectors!!.get(cloudContext.platformVariant)
            val authenticatedContext = connector.authentication().authenticate(cloudContext, request.cloudCredential)
            val instances = request.cloudInstances
            val instanceStatuses = connector.instances().start(authenticatedContext, request.resources, instances)
            val task = statusCheckFactory!!.newPollInstanceStateTask(authenticatedContext, instances,
                    Sets.newHashSet(InstanceStatus.STARTED, InstanceStatus.FAILED))
            var statusResult = InstancesStatusResult(cloudContext, instanceStatuses)
            if (!task.completed(statusResult)) {
                statusResult = syncPollingScheduler!!.schedule(task)
            }
            val result = StartInstancesResult(request, cloudContext, statusResult)
            request.result.onNext(result)
            eventBus!!.notify(result.selector(), Event(event.headers, result))
        } catch (e: Exception) {
            val failure = StartInstancesResult("Failed to start stack", e, request)
            request.result.onNext(failure)
            eventBus!!.notify(failure.selector(), Event(event.headers, failure))
        }

    }

    companion object {

        private val LOGGER = LoggerFactory.getLogger(StartStackHandler::class.java)
    }

}
