package com.sequenceiq.cloudbreak.cloud.template.compute

import java.util.ArrayList
import java.util.HashSet
import java.util.concurrent.Future

import javax.inject.Inject

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationContext
import org.springframework.core.task.AsyncTaskExecutor
import org.springframework.stereotype.Service

import com.sequenceiq.cloudbreak.api.model.AdjustmentType
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext
import com.sequenceiq.cloudbreak.cloud.context.CloudContext
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance
import com.sequenceiq.cloudbreak.cloud.model.CloudResource
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus
import com.sequenceiq.cloudbreak.cloud.model.Group
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus
import com.sequenceiq.cloudbreak.cloud.template.ComputeResourceBuilder
import com.sequenceiq.cloudbreak.cloud.template.context.ResourceBuilderContext
import com.sequenceiq.cloudbreak.cloud.template.init.ResourceBuilders

@Service
class CloudFailureHandler {

    @Inject
    private val resourceBuilderExecutor: AsyncTaskExecutor? = null

    @Inject
    private val applicationContext: ApplicationContext? = null

    fun rollback(auth: AuthenticatedContext, failuresList: List<CloudResourceStatus>, group: Group, fullNodeCount: Int?,
                 ctx: ResourceBuilderContext, resourceBuilders: ResourceBuilders, stx: ScaleContext) {
        if (failuresList.isEmpty()) {
            return
        }
        doRollback(auth, failuresList, group, fullNodeCount, ctx, resourceBuilders, stx)
    }

    private fun doRollback(auth: AuthenticatedContext, failuresList: List<CloudResourceStatus>, group: Group, fullNodeCount: Int?,
                           ctx: ResourceBuilderContext, resourceBuilders: ResourceBuilders, stx: ScaleContext) {
        val localStack = auth.cloudContext
        val failures = failureCount(failuresList)
        if (stx.adjustmentType == null && failures.size > 0) {
            LOGGER.info("Failure policy is null so error will throw")
            throwError(failuresList)
        }
        when (stx.adjustmentType) {
            AdjustmentType.EXACT -> if (stx.threshold > fullNodeCount!! - failures.size) {
                LOGGER.info("Number of failures is more than the threshold so error will throw")
                throwError(failuresList)
            } else if (failures.size != 0) {
                LOGGER.info("Decrease node counts because threshold was higher")
                handleExceptions(auth, failuresList, group, ctx, resourceBuilders, failures, stx.upscale)
            }
            AdjustmentType.PERCENTAGE -> if (java.lang.Double.valueOf(stx.threshold!!.toDouble()) > calculatePercentage(failures.size, fullNodeCount)) {
                LOGGER.info("Number of failures is more than the threshold so error will throw")
                throwError(failuresList)
            } else if (failures.size != 0) {
                LOGGER.info("Decrease node counts because threshold was higher")
                handleExceptions(auth, failuresList, group, ctx, resourceBuilders, failures, stx.upscale)
            }
            AdjustmentType.BEST_EFFORT -> {
                LOGGER.info("Decrease node counts because threshold was higher")
                handleExceptions(auth, failuresList, group, ctx, resourceBuilders, failures, stx.upscale)
            }
            else -> {
                LOGGER.info("Unsupported adjustment type so error will throw")
                throwError(failuresList)
            }
        }
    }

    private fun failureCount(failedResourceRequestResults: List<CloudResourceStatus>): Set<Long> {
        val ids = HashSet<Long>()
        for (failedResourceRequestResult in failedResourceRequestResults) {
            if (ResourceStatus.FAILED == failedResourceRequestResult.status) {
                ids.add(failedResourceRequestResult.privateId)
            }
        }
        return ids
    }

    private fun calculatePercentage(failedResourceRequestResults: Int?, fullNodeCount: Int?): Double {
        return java.lang.Double.valueOf(((fullNodeCount!! + failedResourceRequestResults!!) / fullNodeCount).toDouble()) * ONE_HUNDRED
    }

    private fun handleExceptions(auth: AuthenticatedContext, cloudResourceStatuses: List<CloudResourceStatus>, group: Group,
                                 ctx: ResourceBuilderContext, resourceBuilders: ResourceBuilders, ids: Set<Long>, upscale: Boolean?) {
        val resources = ArrayList<CloudResource>()
        for (exception in cloudResourceStatuses) {
            if (ResourceStatus.FAILED == exception.status || ids.contains(exception.privateId)) {
                LOGGER.error("Failed to create instance: " + exception.statusReason)
                resources.add(exception.cloudResource)
            }
        }
        if (!resources.isEmpty()) {
            LOGGER.info("Resource list not empty so rollback will start.Resource list size is: " + resources.size)
            doRollbackAndDecreaseNodeCount(auth, cloudResourceStatuses, ids, group, ctx, resourceBuilders, upscale)
        }
    }

    private fun doRollbackAndDecreaseNodeCount(auth: AuthenticatedContext, statuses: List<CloudResourceStatus>, ids: Set<Long>, group: Group,
                                               ctx: ResourceBuilderContext, resourceBuilders: ResourceBuilders, upscale: Boolean?) {
        val compute = resourceBuilders.compute(auth.cloudContext.platform)
        val futures = ArrayList<Future<ResourceRequestResult<List<CloudResourceStatus>>>>()
        LOGGER.info(String.format("InstanceGroup %s node count decreased with one so the new node size is: %s", group.name, group.instances.size))
        if (getRemovableInstanceTemplates(group, ids).size <= 0 && (!upscale)!!) {
            LOGGER.info("InstanceGroup node count lower than 1 which is incorrect so error will throw")
            throwError(statuses)
        } else {
            for (i in compute.indices.reversed()) {
                for (cloudResourceStatus in statuses) {
                    try {
                        if (compute[i].resourceType() == cloudResourceStatus.cloudResource.type) {
                            val thread = createThread<ResourceDeleteThread>(ResourceDeleteThread.NAME, ctx, auth, cloudResourceStatus.cloudResource, compute[i], false)
                            val future = resourceBuilderExecutor!!.submit(thread)
                            futures.add(future)
                            for (future1 in futures) {
                                future1.get()
                            }
                            futures.clear()
                        }
                    } catch (e: Exception) {
                        LOGGER.info("Resource can not be deleted. Reason: {} ", e.message)
                    }

                }
            }
        }
    }

    private fun getRemovableInstanceTemplates(group: Group, ids: Set<Long>): List<InstanceTemplate> {
        val instanceTemplates = ArrayList<InstanceTemplate>()
        for (cloudInstance in group.instances) {
            val instanceTemplate = cloudInstance.template
            if (!ids.contains(instanceTemplate.privateId)) {
                instanceTemplates.add(instanceTemplate)
            }
        }
        return instanceTemplates
    }

    private fun <T> createThread(name: String, vararg args: Any): T {
        return applicationContext!!.getBean(name, *args) as T
    }

    private fun throwError(statuses: List<CloudResourceStatus>) {
        throw CloudConnectorException(statuses[0].statusReason)
    }

    class ScaleContext(val upscale: Boolean?, val adjustmentType: AdjustmentType, val threshold: Long?)

    companion object {

        private val LOGGER = LoggerFactory.getLogger(CloudFailureHandler::class.java)
        private val ONE_HUNDRED = 100.0
    }
}
