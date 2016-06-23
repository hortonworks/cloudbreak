package com.sequenceiq.cloudbreak.cloud.task


import javax.inject.Inject

import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.cloud.ResourceConnector
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext
import com.sequenceiq.cloudbreak.cloud.model.CloudResource
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus
import com.sequenceiq.cloudbreak.cloud.transform.ResourceStatusLists

@Component(PollResourcesStateTask.NAME)
@Scope(value = "prototype")
class PollResourcesStateTask
@Inject
constructor(authenticatedContext: AuthenticatedContext, private val resourceConnector: ResourceConnector,
            private val cloudResource: List<CloudResource>, cancellable: Boolean) : AbstractPollTask<ResourcesStatePollerResult>(authenticatedContext, cancellable) {

    @Throws(Exception::class)
    override fun call(): ResourcesStatePollerResult {
        val results = resourceConnector.check(authenticatedContext, cloudResource)
        val status = ResourceStatusLists.aggregate(results)
        return ResourcesStatePollerResult(authenticatedContext.cloudContext, status.status, status.statusReason, results)
    }

    override fun completed(resourcesStatePollerResult: ResourcesStatePollerResult): Boolean {
        return resourcesStatePollerResult.status.isPermanent
    }

    companion object {
        val NAME = "pollResourcesStateTask"
    }
}
