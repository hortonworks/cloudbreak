package com.sequenceiq.cloudbreak.cloud.template.compute

import com.sequenceiq.cloudbreak.cloud.template.compute.CloudFailureHandler.ScaleContext

import java.util.ArrayList
import java.util.Collections
import java.util.HashMap
import java.util.concurrent.ExecutionException
import java.util.concurrent.Future

import javax.inject.Inject

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationContext
import org.springframework.core.task.AsyncTaskExecutor
import org.springframework.stereotype.Service

import com.google.common.collect.Ordering
import com.google.common.primitives.Ints
import com.sequenceiq.cloudbreak.api.model.AdjustmentType
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext
import com.sequenceiq.cloudbreak.cloud.context.CloudContext
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance
import com.sequenceiq.cloudbreak.cloud.model.CloudResource
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus
import com.sequenceiq.cloudbreak.cloud.model.Group
import com.sequenceiq.cloudbreak.cloud.model.Image
import com.sequenceiq.cloudbreak.cloud.model.Platform
import com.sequenceiq.cloudbreak.cloud.scheduler.CancellationException
import com.sequenceiq.cloudbreak.cloud.template.ComputeResourceBuilder
import com.sequenceiq.cloudbreak.cloud.template.context.ResourceBuilderContext
import com.sequenceiq.cloudbreak.cloud.template.init.ResourceBuilders
import com.sequenceiq.cloudbreak.common.type.ResourceType

@Service
class ComputeResourceService {

    @Inject
    private val resourceBuilderExecutor: AsyncTaskExecutor? = null
    @Inject
    private val applicationContext: ApplicationContext? = null
    @Inject
    private val resourceBuilders: ResourceBuilders? = null
    @Inject
    private val cloudFailureHandler: CloudFailureHandler? = null

    @Throws(Exception::class)
    fun buildResourcesForLaunch(ctx: ResourceBuilderContext, auth: AuthenticatedContext, groups: List<Group>, image: Image,
                                adjustmentType: AdjustmentType, threshold: Long?): List<CloudResourceStatus> {
        return buildResources(ctx, auth, groups, image, false, adjustmentType, threshold)
    }

    @Throws(Exception::class)
    fun buildResourcesForUpscale(ctx: ResourceBuilderContext, auth: AuthenticatedContext, groups: List<Group>, image: Image): List<CloudResourceStatus> {
        return buildResources(ctx, auth, groups, image, true, AdjustmentType.BEST_EFFORT, null)
    }

    @Throws(Exception::class)
    private fun buildResources(ctx: ResourceBuilderContext, auth: AuthenticatedContext, groups: List<Group>, image: Image, upscale: Boolean?,
                               adjustmentType: AdjustmentType, threshold: Long?): List<CloudResourceStatus> {
        val results = ArrayList<CloudResourceStatus>()
        val fullNodeCount = getFullNodeCount(groups)

        val cloudContext = auth.cloudContext
        val futures = ArrayList<Future<ResourceRequestResult<List<CloudResourceStatus>>>>()
        val builders = resourceBuilders!!.compute(cloudContext.platform)
        for (group in getOrderedCopy(groups)) {
            val instances = group.instances
            for (i in instances.indices) {
                val thread = createThread<ResourceCreateThread>(ResourceCreateThread.NAME, instances[i].template!!.privateId, group, ctx, auth, image)
                val future = resourceBuilderExecutor!!.submit(thread)
                futures.add(future)
                if (isRequestFullWithCloudPlatform(builders.size, futures.size, ctx)) {
                    val futureResultListMap = waitForRequests(futures)
                    results.addAll(flatList(futureResultListMap[FutureResult.SUCCESS]))
                    results.addAll(flatList(futureResultListMap[FutureResult.FAILED]))
                    cloudFailureHandler!!.rollback(auth, flatList(futureResultListMap[FutureResult.FAILED]), group,
                            fullNodeCount, ctx, resourceBuilders, ScaleContext(upscale, adjustmentType, threshold))
                }
            }
            val futureResultListMap = waitForRequests(futures)
            results.addAll(flatList(futureResultListMap[FutureResult.SUCCESS]))
            results.addAll(flatList(futureResultListMap[FutureResult.FAILED]))
            cloudFailureHandler!!.rollback(auth, flatList(futureResultListMap[FutureResult.FAILED]), group, fullNodeCount, ctx,
                    resourceBuilders, ScaleContext(upscale, adjustmentType, threshold))
        }
        return results
    }

    @Throws(Exception::class)
    fun deleteResources(context: ResourceBuilderContext, auth: AuthenticatedContext,
                        resources: List<CloudResource>, cancellable: Boolean): List<CloudResourceStatus> {
        val results = ArrayList<CloudResourceStatus>()
        val futures = ArrayList<Future<ResourceRequestResult<List<CloudResourceStatus>>>>()
        val platform = auth.cloudContext.platform
        val builders = resourceBuilders!!.compute(platform)
        val numberOfBuilders = builders.size
        for (i in numberOfBuilders - 1 downTo 0) {
            val builder = builders[i]
            val resourceList = getResources(builder.resourceType(), resources)
            for (cloudResource in resourceList) {
                val thread = createThread<ResourceDeleteThread>(ResourceDeleteThread.NAME, context, auth, cloudResource, builder, cancellable)
                val future = resourceBuilderExecutor!!.submit(thread)
                futures.add(future)
                if (isRequestFull(futures.size, context)) {
                    results.addAll(flatList(waitForRequests(futures)[FutureResult.SUCCESS]))
                }
            }
            // wait for builder type to finish before starting the next one
            results.addAll(flatList(waitForRequests(futures)[FutureResult.SUCCESS]))
        }
        return results
    }

    @Throws(Exception::class)
    fun stopInstances(context: ResourceBuilderContext, auth: AuthenticatedContext,
                      resources: List<CloudResource>, cloudInstances: List<CloudInstance>): List<CloudVmInstanceStatus> {
        return stopStart(context, auth, resources, cloudInstances)
    }

    @Throws(Exception::class)
    fun startInstances(context: ResourceBuilderContext, auth: AuthenticatedContext,
                       resources: List<CloudResource>, cloudInstances: List<CloudInstance>): List<CloudVmInstanceStatus> {
        return stopStart(context, auth, resources, cloudInstances)
    }

    private fun getFullNodeCount(groups: List<Group>): Int {
        var fullNodeCount = 0
        for (group in groups) {
            fullNodeCount += group.instances.size
        }
        return fullNodeCount
    }

    @Throws(Exception::class)
    private fun stopStart(context: ResourceBuilderContext,
                          auth: AuthenticatedContext, resources: List<CloudResource>, instances: List<CloudInstance>): List<CloudVmInstanceStatus> {
        val results = ArrayList<CloudVmInstanceStatus>()
        val futures = ArrayList<Future<ResourceRequestResult<List<CloudVmInstanceStatus>>>>()
        val platform = auth.cloudContext.platform
        val builders = resourceBuilders!!.compute(platform)
        if (!context.isBuild) {
            Collections.reverse(builders)
        }
        for (builder in builders) {
            val resourceList = getResources(builder.resourceType(), resources)
            for (cloudResource in resourceList) {
                val instance = getCloudInstance(cloudResource, instances)
                if (instance != null) {
                    val thread = createThread<ResourceStopStartThread>(ResourceStopStartThread.NAME, context, auth, cloudResource, instance, builder)
                    val future = resourceBuilderExecutor!!.submit(thread)
                    futures.add(future)
                    if (isRequestFull(futures.size, context)) {
                        results.addAll(flatVmList(waitForRequests(futures)[FutureResult.SUCCESS]))
                    }
                } else {
                    break
                }
            }
        }
        results.addAll(flatVmList(waitForRequests(futures)[FutureResult.SUCCESS]))
        return results
    }

    @Throws(Exception::class)
    private fun <T> waitForRequests(futures: MutableList<Future<ResourceRequestResult<T>>>): Map<FutureResult, List<T>> {
        val result = HashMap<FutureResult, List<T>>()
        result.put(FutureResult.FAILED, ArrayList<T>())
        result.put(FutureResult.SUCCESS, ArrayList<T>())
        val requests = futures.size
        LOGGER.info("Waiting for {} requests to finish", requests)
        try {
            for (future in futures) {
                val resourceRequestResult = future.get()
                if (FutureResult.FAILED == resourceRequestResult.status) {
                    result[FutureResult.FAILED].add(resourceRequestResult.result)
                } else {
                    result[FutureResult.SUCCESS].add(resourceRequestResult.result)
                }
            }
        } catch (e: InterruptedException) {
            if (e is CancellationException) {
                throw e
            } else {
                LOGGER.error("Failed to execute the request", e)
            }
        } catch (e: ExecutionException) {
            if (e is CancellationException) {
                throw e
            } else {
                LOGGER.error("Failed to execute the request", e)
            }
        }

        LOGGER.info("{} requests have finished, continue with next group", requests)
        futures.clear()
        return result
    }

    private fun isRequestFull(runningRequests: Int, context: ResourceBuilderContext): Boolean {
        return isRequestFullWithCloudPlatform(1, runningRequests, context)
    }

    private fun isRequestFullWithCloudPlatform(numberOfBuilders: Int, runningRequests: Int, context: ResourceBuilderContext): Boolean {
        return runningRequests * numberOfBuilders % context.parallelResourceRequest == 0
    }

    private fun getResources(resourceType: ResourceType, resources: List<CloudResource>): List<CloudResource> {
        val selected = ArrayList<CloudResource>()
        for (resource in resources) {
            if (resourceType === resource.type) {
                selected.add(resource)
            }
        }
        return selected
    }

    private fun getOrderedCopy(groups: List<Group>): List<Group> {
        val byLengthOrdering = object : Ordering<Group>() {
            override fun compare(left: Group?, right: Group?): Int {
                return Ints.compare(left!!.instances.size, right!!.instances.size)
            }
        }
        return byLengthOrdering.sortedCopy(groups)
    }

    private fun <T> createThread(name: String, vararg args: Any): T {
        return applicationContext!!.getBean(name, *args) as T
    }

    private fun getCloudInstance(cloudResource: CloudResource, instances: List<CloudInstance>): CloudInstance? {
        for (instance in instances) {
            if (instance.instanceId!!.equals(cloudResource.name, ignoreCase = true) || instance.instanceId!!.equals(cloudResource.reference, ignoreCase = true)) {
                return instance
            }
        }
        return null
    }

    private fun flatVmList(lists: List<List<CloudVmInstanceStatus>>): List<CloudVmInstanceStatus> {
        val result = ArrayList<CloudVmInstanceStatus>()
        for (list in lists) {
            result.addAll(list)
        }
        return result
    }

    private fun flatList(lists: List<List<CloudResourceStatus>>): List<CloudResourceStatus> {
        val result = ArrayList<CloudResourceStatus>()
        for (list in lists) {
            result.addAll(list)
        }
        return result
    }

    companion object {

        private val LOGGER = LoggerFactory.getLogger(ComputeResourceService::class.java)
    }

}
