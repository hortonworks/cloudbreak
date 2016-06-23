package com.sequenceiq.cloudbreak.cloud.template.task

import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext
import com.sequenceiq.cloudbreak.cloud.template.context.ResourceBuilderContext
import com.sequenceiq.cloudbreak.cloud.model.CloudResource
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus
import com.sequenceiq.cloudbreak.cloud.task.AbstractPollTask
import com.sequenceiq.cloudbreak.cloud.template.ResourceChecker

@Component(PollResourceTask.NAME)
@Scope(value = "prototype")
class PollResourceTask(authenticatedContext: AuthenticatedContext, private val checker: ResourceChecker<ResourceBuilderContext>,
                       private val cloudResources: List<CloudResource>, private val context: ResourceBuilderContext, cancellable: Boolean) : AbstractPollTask<List<CloudResourceStatus>>(authenticatedContext, cancellable) {

    @Throws(Exception::class)
    override fun call(): List<CloudResourceStatus> {
        return checker.checkResources(context, authenticatedContext, cloudResources)
    }

    override fun completed(resourceStatuses: List<CloudResourceStatus>): Boolean {
        for (resourceStatus in resourceStatuses) {
            if (resourceStatus.status!!.isTransient) {
                return false
            }
        }
        return true
    }

    companion object {
        val NAME = "pollResourceTask"
    }

}
