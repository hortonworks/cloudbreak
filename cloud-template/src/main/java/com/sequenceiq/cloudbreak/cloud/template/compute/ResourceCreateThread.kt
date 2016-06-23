package com.sequenceiq.cloudbreak.cloud.template.compute

import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext
import com.sequenceiq.cloudbreak.cloud.model.CloudResource
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus
import com.sequenceiq.cloudbreak.cloud.model.Group
import com.sequenceiq.cloudbreak.cloud.model.Image
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier
import com.sequenceiq.cloudbreak.cloud.scheduler.CancellationException
import com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup
import com.sequenceiq.cloudbreak.cloud.scheduler.SyncPollingScheduler
import com.sequenceiq.cloudbreak.cloud.store.InMemoryStateStore
import com.sequenceiq.cloudbreak.cloud.task.PollTask
import com.sequenceiq.cloudbreak.cloud.template.ComputeResourceBuilder
import com.sequenceiq.cloudbreak.cloud.template.context.ResourceBuilderContext
import com.sequenceiq.cloudbreak.cloud.template.init.ResourceBuilders
import com.sequenceiq.cloudbreak.cloud.template.task.ResourcePollTaskFactory
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

import javax.inject.Inject
import java.util.ArrayList
import java.util.concurrent.Callable

import com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup.CANCELLED
import java.lang.String.format

@Component(ResourceCreateThread.NAME)
@Scope(value = "prototype")
class ResourceCreateThread(private val privateId: Long, private val group: Group, private val context: ResourceBuilderContext, private val auth: AuthenticatedContext, private val image: Image) : Callable<ResourceRequestResult<List<CloudResourceStatus>>> {

    @Inject
    private val resourceBuilders: ResourceBuilders? = null
    @Inject
    private val syncPollingScheduler: SyncPollingScheduler<List<CloudResourceStatus>>? = null
    @Inject
    private val resourcePollTaskFactory: ResourcePollTaskFactory? = null
    @Inject
    private val resourceNotifier: PersistenceNotifier? = null

    @Throws(Exception::class)
    override fun call(): ResourceRequestResult<List<CloudResourceStatus>> {
        val results = ArrayList<CloudResourceStatus>()
        val buildableResources = ArrayList<CloudResource>()
        try {
            for (builder in resourceBuilders!!.compute(auth.cloudContext.platform)) {
                LOGGER.info("Building {} resources of {} instance group", builder.resourceType(), group.name)
                val list = builder.create(context, privateId, auth, group, image)
                buildableResources.addAll(list)
                createResource(auth, list)

                val pollGroup = InMemoryStateStore.getStack(auth.cloudContext.id)
                if (pollGroup != null && CANCELLED == pollGroup) {
                    throw CancellationException(format("Building of %s has been cancelled", list))
                }

                val resources = builder.build(context, privateId, auth, group, image, list)
                updateResource(auth, resources)
                context.addComputeResources(privateId, resources)
                val task = resourcePollTaskFactory!!.newPollResourceTask(builder, auth, resources, context, true)
                val pollerResult = syncPollingScheduler!!.schedule(task)
                for (resourceStatus in pollerResult) {
                    resourceStatus.privateId = privateId
                }
                results.addAll(pollerResult)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            LOGGER.error("", e)
            results.clear()
            for (buildableResource in buildableResources) {
                results.add(CloudResourceStatus(buildableResource, ResourceStatus.FAILED, e.message, privateId))
            }
            return ResourceRequestResult(FutureResult.FAILED, results)
        }

        return ResourceRequestResult(FutureResult.SUCCESS, results)
    }

    @Throws(Exception::class)
    private fun createResource(auth: AuthenticatedContext, cloudResources: List<CloudResource>): List<CloudResource> {
        for (cloudResource in cloudResources) {
            if (cloudResource.isPersistent) {
                resourceNotifier!!.notifyAllocation(cloudResource, auth.cloudContext)
            }
        }
        return cloudResources
    }

    @Throws(Exception::class)
    private fun updateResource(auth: AuthenticatedContext, cloudResources: List<CloudResource>): List<CloudResource> {
        for (cloudResource in cloudResources) {
            if (cloudResource.isPersistent) {
                resourceNotifier!!.notifyUpdate(cloudResource, auth.cloudContext)
            }
        }
        return cloudResources
    }

    companion object {

        val NAME = "resourceCreateThread"
        private val LOGGER = LoggerFactory.getLogger(ResourceCreateThread::class.java)
    }
}
