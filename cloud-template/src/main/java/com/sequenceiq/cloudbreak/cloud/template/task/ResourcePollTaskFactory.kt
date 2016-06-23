package com.sequenceiq.cloudbreak.cloud.template.task

import javax.inject.Inject

import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext
import com.sequenceiq.cloudbreak.cloud.template.context.ResourceBuilderContext
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance
import com.sequenceiq.cloudbreak.cloud.model.CloudResource
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus
import com.sequenceiq.cloudbreak.cloud.task.PollTask
import com.sequenceiq.cloudbreak.cloud.template.ComputeResourceBuilder
import com.sequenceiq.cloudbreak.cloud.template.ResourceChecker

@Component
class ResourcePollTaskFactory {
    @Inject
    private val applicationContext: ApplicationContext? = null

    fun newPollResourceTask(checker: ResourceChecker<ResourceBuilderContext>, authenticatedContext: AuthenticatedContext,
                            cloudResource: List<CloudResource>, context: ResourceBuilderContext, cancellable: Boolean): PollTask<List<CloudResourceStatus>> {
        return createPollTask(PollResourceTask.NAME, authenticatedContext, checker, cloudResource, context, cancellable)
    }

    fun newPollComputeStatusTask(builder: ComputeResourceBuilder<ResourceBuilderContext>, authenticatedContext: AuthenticatedContext,
                                 context: ResourceBuilderContext, instance: CloudInstance): PollTask<List<CloudVmInstanceStatus>> {
        return createPollTask(PollComputeStatusTask.NAME, authenticatedContext, builder, context, instance)
    }

    @SuppressWarnings("unchecked")
    private fun <T> createPollTask(name: String, vararg args: Any): T {
        return applicationContext!!.getBean(name, *args) as T
    }
}
