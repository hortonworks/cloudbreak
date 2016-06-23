package com.sequenceiq.cloudbreak.cloud.handler

import javax.inject.Inject

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.cloud.CloudConnector
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext
import com.sequenceiq.cloudbreak.cloud.context.CloudContext
import com.sequenceiq.cloudbreak.cloud.event.resource.LaunchStackRequest
import com.sequenceiq.cloudbreak.cloud.event.resource.LaunchStackResult
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors
import com.sequenceiq.cloudbreak.cloud.model.CloudCredentialStatus
import com.sequenceiq.cloudbreak.cloud.model.CloudResource
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus
import com.sequenceiq.cloudbreak.cloud.model.CredentialStatus
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
class LaunchStackHandler : CloudPlatformEventHandler<LaunchStackRequest> {

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

    override fun type(): Class<LaunchStackRequest> {
        return LaunchStackRequest::class.java
    }

    override fun accept(launchStackRequestEvent: Event<LaunchStackRequest>) {
        LOGGER.info("Received event: {}", launchStackRequestEvent)
        val request = launchStackRequestEvent.data
        val cloudContext = request.cloudContext
        try {
            val connector = cloudPlatformConnectors!!.get(cloudContext.platformVariant)
            val ac = connector.authentication().authenticate(cloudContext, request.cloudCredential)
            val credentialStatus = connector.credentials().create(ac)
            if (CredentialStatus.FAILED === credentialStatus.status) {
                if (credentialStatus.exception != null) {
                    throw CloudConnectorException(credentialStatus.exception)
                }
                throw CloudConnectorException(credentialStatus.statusReason)
            }
            val resourceStatus = connector.resources().launch(ac, request.cloudStack, persistenceNotifier,
                    request.adjustmentType, request.threshold)
            val resources = ResourceLists.transform(resourceStatus)
            val task = statusCheckFactory!!.newPollResourcesStateTask(ac, resources, true)
            var statePollerResult = ResourcesStatePollerResults.build(cloudContext, resourceStatus)
            if (!task.completed(statePollerResult)) {
                statePollerResult = syncPollingScheduler!!.schedule(task)
            }
            val result = ResourcesStatePollerResults.transformToLaunchStackResult(request, statePollerResult)
            request.result.onNext(result)
            eventBus!!.notify(result.selector(), Event(launchStackRequestEvent.headers, result))
            LOGGER.info("Launching the stack successfully finished for {}", cloudContext)
        } catch (e: Exception) {
            val failure = LaunchStackResult(e, request)
            request.result.onNext(failure)
            eventBus!!.notify(failure.selector(), Event(launchStackRequestEvent.headers, failure))
        }

    }

    companion object {

        private val LOGGER = LoggerFactory.getLogger(LaunchStackHandler::class.java)
    }
}
