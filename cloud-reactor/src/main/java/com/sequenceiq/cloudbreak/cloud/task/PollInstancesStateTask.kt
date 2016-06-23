package com.sequenceiq.cloudbreak.cloud.task

import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

import com.google.common.collect.Sets
import com.sequenceiq.cloudbreak.cloud.InstanceConnector
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext
import com.sequenceiq.cloudbreak.cloud.event.instance.InstancesStatusResult
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus

@Component(PollInstancesStateTask.NAME)
@Scope(value = "prototype")
class PollInstancesStateTask @JvmOverloads constructor(authenticatedContext: AuthenticatedContext, private val instanceConnector: InstanceConnector, private val instances: List<CloudInstance>,
                                                       private val completedStatuses: Set<InstanceStatus> = Sets.newHashSet<InstanceStatus>()) : AbstractPollTask<InstancesStatusResult>(authenticatedContext) {

    @Throws(Exception::class)
    override fun call(): InstancesStatusResult {
        val instanceStatuses = instanceConnector.check(authenticatedContext, instances)
        return InstancesStatusResult(authenticatedContext.cloudContext, instanceStatuses)
    }

    override fun completed(instancesStatusResult: InstancesStatusResult): Boolean {
        for (result in instancesStatusResult.results) {
            if (result.status.isTransient || !completedStatuses.isEmpty() && !completedStatuses.contains(result.status)) {
                return false
            }
        }
        return true
    }

    companion object {
        val NAME = "pollInstancesStateTask"
    }
}
