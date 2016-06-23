package com.sequenceiq.cloudbreak.cloud.template.compute

import com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup.CANCELLED
import java.util.Arrays.asList
import java.util.concurrent.Callable

import javax.inject.Inject

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext
import com.sequenceiq.cloudbreak.cloud.template.context.ResourceBuilderContext
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance
import com.sequenceiq.cloudbreak.cloud.model.CloudResource
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus
import com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup
import com.sequenceiq.cloudbreak.cloud.scheduler.SyncPollingScheduler
import com.sequenceiq.cloudbreak.cloud.store.InMemoryStateStore
import com.sequenceiq.cloudbreak.cloud.task.PollTask
import com.sequenceiq.cloudbreak.cloud.template.ComputeResourceBuilder
import com.sequenceiq.cloudbreak.cloud.template.task.ResourcePollTaskFactory

@Component(ResourceStopStartThread.NAME)
@Scope(value = "prototype")
class ResourceStopStartThread(private val context: ResourceBuilderContext, private val auth: AuthenticatedContext,
                              private val resource: CloudResource, private val instance: CloudInstance, private val builder: ComputeResourceBuilder<ResourceBuilderContext>) : Callable<ResourceRequestResult<List<CloudVmInstanceStatus>>> {

    @Inject
    private val syncPollingScheduler: SyncPollingScheduler<List<CloudVmInstanceStatus>>? = null
    @Inject
    private val resourcePollTaskFactory: ResourcePollTaskFactory? = null

    @Throws(Exception::class)
    override fun call(): ResourceRequestResult<List<CloudVmInstanceStatus>> {
        LOGGER.info("{} compute resource {}", if (context.isBuild) "Starting" else "Stopping", resource)
        val pollGroup = InMemoryStateStore.getStack(auth.cloudContext.id)
        if (pollGroup != null && CANCELLED == pollGroup) {
            val result = createResult(InstanceStatus.UNKNOWN)
            return ResourceRequestResult(FutureResult.SUCCESS, result)
        }
        val status = if (context.isBuild) builder.start(context, auth, instance) else builder.stop(context, auth, instance)
        if (status != null) {
            val task = resourcePollTaskFactory!!.newPollComputeStatusTask(builder, auth, context, status.cloudInstance)
            val pollResult = syncPollingScheduler!!.schedule(task)
            return ResourceRequestResult(FutureResult.SUCCESS, pollResult)
        }
        return ResourceRequestResult(FutureResult.SUCCESS, createResult(InstanceStatus.UNKNOWN))
    }

    private fun createResult(status: InstanceStatus): List<CloudVmInstanceStatus> {
        return asList(CloudVmInstanceStatus(instance, status))
    }

    companion object {

        val NAME = "resourceStopStartThread"
        private val LOGGER = LoggerFactory.getLogger(ResourceStopStartThread::class.java)
    }

}
