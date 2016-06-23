package com.sequenceiq.cloudbreak.cloud.task

import javax.inject.Inject

import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.cloud.CloudConnector
import com.sequenceiq.cloudbreak.cloud.InstanceConnector
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext
import com.sequenceiq.cloudbreak.cloud.event.instance.InstanceConsoleOutputResult
import com.sequenceiq.cloudbreak.cloud.event.instance.InstancesStatusResult
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance
import com.sequenceiq.cloudbreak.cloud.model.CloudResource
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus

@Component
class PollTaskFactory {
    @Inject
    private val applicationContext: ApplicationContext? = null
    @Inject
    private val cloudPlatformConnectors: CloudPlatformConnectors? = null

    fun newPollResourcesStateTask(authenticatedContext: AuthenticatedContext,
                                  cloudResource: List<CloudResource>, cancellable: Boolean): PollTask<ResourcesStatePollerResult> {
        val connector = cloudPlatformConnectors!!.get(authenticatedContext.cloudContext.platformVariant)
        return createPollTask(PollResourcesStateTask.NAME, authenticatedContext, connector.resources(), cloudResource, cancellable)
    }

    fun newPollInstanceStateTask(authenticatedContext: AuthenticatedContext, instances: List<CloudInstance>,
                                 completedStatuses: Set<InstanceStatus>): PollTask<InstancesStatusResult> {
        val connector = cloudPlatformConnectors!!.get(authenticatedContext.cloudContext.platformVariant)
        return createPollTask(PollInstancesStateTask.NAME, authenticatedContext, connector.instances(), instances, completedStatuses)
    }

    fun newPollConsoleOutputTask(instanceConnector: InstanceConnector,
                                 authenticatedContext: AuthenticatedContext, instance: CloudInstance): PollTask<InstanceConsoleOutputResult> {
        return createPollTask(PollInstanceConsoleOutputTask.NAME, instanceConnector, authenticatedContext, instance)
    }


    @SuppressWarnings("unchecked")
    private fun <T> createPollTask(name: String, vararg args: Any): T {
        return applicationContext!!.getBean(name, *args) as T
    }
}
