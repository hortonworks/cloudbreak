package com.sequenceiq.cloudbreak.cloud.handler

import javax.inject.Inject

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.cloud.CloudConnector
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext
import com.sequenceiq.cloudbreak.cloud.event.resource.TerminateStackRequest
import com.sequenceiq.cloudbreak.cloud.event.resource.TerminateStackResult
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors
import com.sequenceiq.cloudbreak.cloud.model.CloudCredentialStatus
import com.sequenceiq.cloudbreak.cloud.model.CloudResource
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus
import com.sequenceiq.cloudbreak.cloud.model.CredentialStatus
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus
import com.sequenceiq.cloudbreak.cloud.scheduler.SyncPollingScheduler
import com.sequenceiq.cloudbreak.cloud.task.PollTask
import com.sequenceiq.cloudbreak.cloud.task.PollTaskFactory
import com.sequenceiq.cloudbreak.cloud.task.ResourcesStatePollerResult
import com.sequenceiq.cloudbreak.cloud.transform.ResourceLists
import com.sequenceiq.cloudbreak.cloud.transform.ResourcesStatePollerResults

import reactor.bus.Event
import reactor.bus.EventBus

@Component
class TerminateStackHandler : CloudPlatformEventHandler<TerminateStackRequest<Any>> {

    @Inject
    private val cloudPlatformConnectors: CloudPlatformConnectors? = null
    @Inject
    private val statusCheckFactory: PollTaskFactory? = null
    @Inject
    private val syncPollingScheduler: SyncPollingScheduler<ResourcesStatePollerResult>? = null
    @Inject
    private val eventBus: EventBus? = null

    override fun type(): Class<TerminateStackRequest<Any>> {
        return TerminateStackRequest<Any>::class.java
    }

    override fun accept(terminateStackRequestEvent: Event<TerminateStackRequest<Any>>) {
        LOGGER.info("Received event: {}", terminateStackRequestEvent)
        val request = terminateStackRequestEvent.data
        try {
            val connector = cloudPlatformConnectors!!.get(request.cloudContext.platformVariant)
            val ac = connector.authentication().authenticate(request.cloudContext, request.cloudCredential)
            val resourceStatus = connector.resources().terminate(ac, request.cloudStack, request.cloudResources)
            val resources = ResourceLists.transform(resourceStatus)
            val result: TerminateStackResult
            if (!resources.isEmpty()) {
                val task = statusCheckFactory!!.newPollResourcesStateTask(ac, resources, false)
                var statePollerResult = ResourcesStatePollerResults.build(request.cloudContext, resourceStatus)
                if (!task.completed(statePollerResult)) {
                    statePollerResult = syncPollingScheduler!!.schedule(task)
                }
                if (statePollerResult.status != ResourceStatus.DELETED) {
                    throw CloudConnectorException("Stack could not be terminated, Resource(s) could not be deleted on the provider side.")
                } else {
                    result = TerminateStackResult(request)
                }
            } else {
                result = TerminateStackResult(request)
            }
            val credentialStatus = connector.credentials().delete(ac)
            if (CredentialStatus.FAILED === credentialStatus.status) {
                if (credentialStatus.exception != null) {
                    throw CloudConnectorException(credentialStatus.exception)
                }
                throw CloudConnectorException(credentialStatus.statusReason)
            }
            request.result.onNext(result)
            LOGGER.info("TerminateStackHandler finished")
            eventBus!!.notify(result.selector(), Event(terminateStackRequestEvent.headers, result))
        } catch (e: Exception) {
            LOGGER.error("Failed to handle TerminateStackRequest: {}", e)
            val terminateStackResult = TerminateStackResult("Stack termination failed.", e, request)
            request.result.onNext(terminateStackResult)
            eventBus!!.notify(terminateStackResult.selector(), Event(terminateStackRequestEvent.headers, terminateStackResult))
        }

    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(TerminateStackHandler::class.java)
    }
}
