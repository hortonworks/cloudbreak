package com.sequenceiq.cloudbreak.cloud.handler

import javax.inject.Inject

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.cloud.CloudConnector
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext
import com.sequenceiq.cloudbreak.cloud.context.CloudContext
import com.sequenceiq.cloudbreak.cloud.event.resource.UpscaleStackRequest
import com.sequenceiq.cloudbreak.cloud.event.resource.UpscaleStackResult
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors
import com.sequenceiq.cloudbreak.cloud.model.CloudResource
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier
import com.sequenceiq.cloudbreak.cloud.scheduler.SyncPollingScheduler
import com.sequenceiq.cloudbreak.cloud.task.PollTask
import com.sequenceiq.cloudbreak.cloud.task.PollTaskFactory
import com.sequenceiq.cloudbreak.cloud.task.ResourcesStatePollerResult
import com.sequenceiq.cloudbreak.cloud.transform.ResourceLists
import com.sequenceiq.cloudbreak.cloud.transform.ResourcesStatePollerResults

import reactor.bus.Event
import reactor.bus.EventBus

@Component
class UpscaleStackHandler : CloudPlatformEventHandler<UpscaleStackRequest<Any>> {

    @Inject
    private val cloudPlatformConnectors: CloudPlatformConnectors? = null
    @Inject
    private val syncPollingScheduler: SyncPollingScheduler<ResourcesStatePollerResult>? = null
    @Inject
    private val statusCheckFactory: PollTaskFactory? = null
    @Inject
    private val persistenceNotifier: PersistenceNotifier? = null
    @Inject
    private val eventBus: EventBus? = null

    override fun type(): Class<UpscaleStackRequest<Any>> {
        return UpscaleStackRequest<Any>::class.java
    }

    override fun accept(upscaleStackRequestEvent: Event<UpscaleStackRequest<Any>>) {
        LOGGER.info("Received event: {}", upscaleStackRequestEvent)
        val request = upscaleStackRequestEvent.data
        val cloudContext = request.cloudContext
        try {
            val connector = cloudPlatformConnectors!!.get(cloudContext.platformVariant)
            val ac = connector.authentication().authenticate(cloudContext, request.cloudCredential)
            val resourceStatus = connector.resources().upscale(ac, request.cloudStack, request.resourceList)
            val resources = ResourceLists.transform(resourceStatus)
            val task = statusCheckFactory!!.newPollResourcesStateTask(ac, resources, true)
            var statePollerResult = ResourcesStatePollerResults.build(cloudContext, resourceStatus)
            if (!task.completed(statePollerResult)) {
                statePollerResult = syncPollingScheduler!!.schedule(task)
            }
            val result = ResourcesStatePollerResults.transformToUpscaleStackResult(statePollerResult, request)
            request.result.onNext(result)
            eventBus!!.notify(result.selector(), Event(upscaleStackRequestEvent.headers, result))
            LOGGER.info("Upscale successfully finished for {}", cloudContext)
        } catch (e: Exception) {
            val result = UpscaleStackResult(e.message, e, request)
            request.result.onNext(result)
            eventBus!!.notify(result.failureSelector(UpscaleStackResult::class.java), Event(upscaleStackRequestEvent.headers, result))
        }

    }

    companion object {

        private val LOGGER = LoggerFactory.getLogger(UpscaleStackHandler::class.java)
    }

}
