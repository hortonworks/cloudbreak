package com.sequenceiq.cloudbreak.cloud.template.task

import java.util.Arrays.asList

import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext
import com.sequenceiq.cloudbreak.cloud.template.context.ResourceBuilderContext
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus
import com.sequenceiq.cloudbreak.cloud.task.AbstractPollTask
import com.sequenceiq.cloudbreak.cloud.template.ComputeResourceBuilder

@Component(PollComputeStatusTask.NAME)
@Scope(value = "prototype")
class PollComputeStatusTask(auth: AuthenticatedContext, private val builder: ComputeResourceBuilder<ResourceBuilderContext>, private val context: ResourceBuilderContext, private val instance: CloudInstance) : AbstractPollTask<List<CloudVmInstanceStatus>>(auth) {

    @Throws(Exception::class)
    override fun call(): List<CloudVmInstanceStatus> {
        return builder.checkInstances(context, authenticatedContext, asList(instance))
    }

    override fun completed(status: List<CloudVmInstanceStatus>): Boolean {
        for (result in status) {
            if (result.status.isTransient) {
                return false
            }
        }
        return true
    }

    companion object {
        val NAME = "pollComputeStatusTask"
    }

}
