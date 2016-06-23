package com.sequenceiq.cloudbreak.cloud.template.compute

import java.util.Arrays.asList
import java.util.concurrent.Callable

import javax.inject.Inject

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext
import com.sequenceiq.cloudbreak.cloud.template.context.ResourceBuilderContext
import com.sequenceiq.cloudbreak.cloud.model.CloudResource
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier
import com.sequenceiq.cloudbreak.cloud.scheduler.SyncPollingScheduler
import com.sequenceiq.cloudbreak.cloud.task.PollTask
import com.sequenceiq.cloudbreak.cloud.template.ComputeResourceBuilder
import com.sequenceiq.cloudbreak.cloud.template.task.ResourcePollTaskFactory
import com.sequenceiq.cloudbreak.common.type.CommonStatus

@Component(ResourceDeleteThread.NAME)
@Scope(value = "prototype")
class ResourceDeleteThread(private val context: ResourceBuilderContext, private val auth: AuthenticatedContext,
                           private val resource: CloudResource, private val builder: ComputeResourceBuilder<ResourceBuilderContext>, private val cancellable: Boolean) : Callable<ResourceRequestResult<List<CloudResourceStatus>>> {

    @Inject
    private val syncPollingScheduler: SyncPollingScheduler<List<CloudResourceStatus>>? = null
    @Inject
    private val resourcePollTaskFactory: ResourcePollTaskFactory? = null
    @Inject
    private val resourceNotifier: PersistenceNotifier? = null

    @Throws(Exception::class)
    override fun call(): ResourceRequestResult<List<CloudResourceStatus>> {
        LOGGER.info("Deleting compute resource {}", resource)
        if (resource.status === CommonStatus.CREATED) {
            val deletedResource = builder.delete(context, auth, resource)
            if (deletedResource != null) {
                val task = resourcePollTaskFactory!!.newPollResourceTask(builder, auth, asList(deletedResource), context, cancellable)
                val pollerResult = syncPollingScheduler!!.schedule(task)
                deleteResource()
                return ResourceRequestResult(FutureResult.SUCCESS, pollerResult)
            }
        }
        deleteResource()
        val status = CloudResourceStatus(resource, ResourceStatus.DELETED)
        return ResourceRequestResult(FutureResult.SUCCESS, asList(status))
    }

    @Throws(InterruptedException::class)
    private fun deleteResource() {
        resourceNotifier!!.notifyDeletion(resource, auth.cloudContext)
    }

    companion object {

        val NAME = "resourceDeleteThread"
        private val LOGGER = LoggerFactory.getLogger(ResourceDeleteThread::class.java)
    }

}
