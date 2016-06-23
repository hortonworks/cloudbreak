package com.sequenceiq.cloudbreak.cloud.handler

import javax.inject.Inject

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.cloud.CloudConnector
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext
import com.sequenceiq.cloudbreak.cloud.context.CloudContext
import com.sequenceiq.cloudbreak.cloud.event.resource.DownscaleStackRequest
import com.sequenceiq.cloudbreak.cloud.event.resource.DownscaleStackResult
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors
import com.sequenceiq.cloudbreak.cloud.model.CloudResource
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus
import com.sequenceiq.cloudbreak.cloud.scheduler.SyncPollingScheduler
import com.sequenceiq.cloudbreak.cloud.task.PollTask
import com.sequenceiq.cloudbreak.cloud.task.PollTaskFactory
import com.sequenceiq.cloudbreak.cloud.task.ResourcesStatePollerResult
import com.sequenceiq.cloudbreak.cloud.transform.ResourceLists
import com.sequenceiq.cloudbreak.cloud.transform.ResourcesStatePollerResults

import reactor.bus.Event
import reactor.bus.EventBus

@Component("DownscaleStackHandler")
class DownscaleStackHandler : DownscaleStackExecuter, CloudPlatformEventHandler<DownscaleStackRequest<Any>> {

    @Inject
    private val syncPollingScheduler: SyncPollingScheduler<ResourcesStatePollerResult>? = null
    @Inject
    private val statusCheckFactory: PollTaskFactory? = null
    @Inject
    private val cloudPlatformConnectors: CloudPlatformConnectors? = null
    @Inject
    private val eventBus: EventBus? = null

    override fun type(): Class<DownscaleStackRequest<Any>> {
        return DownscaleStackRequest<Any>::class.java
    }

    override fun accept(downscaleStackRequestEvent: Event<DownscaleStackRequest<Any>>) {
        LOGGER.info("Received event: {}", downscaleStackRequestEvent)
        val request = downscaleStackRequestEvent.data
        val result = execute(request)
        LOGGER.info("DownscaleStackRequest finished")
        eventBus!!.notify(result.selector(), Event(downscaleStackRequestEvent.headers, result))
    }

    override fun execute(request: DownscaleStackRequest<Any>): DownscaleStackResult {
        val result: DownscaleStackResult
        try {
            val cloudContext = request.cloudContext
            val connector = cloudPlatformConnectors!!.get(cloudContext.platformVariant)
            val ac = connector.authentication().authenticate(cloudContext, request.cloudCredential)
            val resourceStatus = connector.resources().downscale(ac, request.cloudStack, request.cloudResources,
                    request.instances)
            val resources = ResourceLists.transform(resourceStatus)
            val task = statusCheckFactory!!.newPollResourcesStateTask(ac, resources, true)
            var statePollerResult = ResourcesStatePollerResults.build(cloudContext, resourceStatus)
            if (!task.completed(statePollerResult)) {
                statePollerResult = syncPollingScheduler!!.schedule(task)
            }
            LOGGER.info("Downscale successfully finished for {}", cloudContext)
            result = DownscaleStackResult(request, ResourceLists.transform(statePollerResult.results))
        } catch (e: Exception) {
            LOGGER.error("Failed to handle DownscaleStackRequest.", e)
            result = DownscaleStackResult(e.message, e, request)
        }

        request.result.onNext(result)
        return result
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(DownscaleStackHandler::class.java)
    }
}
