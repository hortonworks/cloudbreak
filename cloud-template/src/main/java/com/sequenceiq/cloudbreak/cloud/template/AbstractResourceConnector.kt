package com.sequenceiq.cloudbreak.cloud.template

import java.util.ArrayList
import java.util.Collections

import javax.inject.Inject

import com.sequenceiq.cloudbreak.api.model.AdjustmentType
import com.sequenceiq.cloudbreak.cloud.ResourceConnector
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext
import com.sequenceiq.cloudbreak.cloud.context.CloudContext
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance
import com.sequenceiq.cloudbreak.cloud.model.CloudResource
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus
import com.sequenceiq.cloudbreak.cloud.model.CloudStack
import com.sequenceiq.cloudbreak.cloud.model.Group
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate
import com.sequenceiq.cloudbreak.cloud.model.Platform
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier
import com.sequenceiq.cloudbreak.cloud.template.compute.ComputeResourceService
import com.sequenceiq.cloudbreak.cloud.template.context.ResourceBuilderContext
import com.sequenceiq.cloudbreak.cloud.template.init.ContextBuilders
import com.sequenceiq.cloudbreak.cloud.template.network.NetworkResourceService

/**
 * Abstract base implementation of [ResourceConnector] for cloud provider which do not have template based deployments. It provides the
 * functionality to call the resource builders in order starting from the [NetworkResourceBuilder] and continueing with the
 * [ComputeResourceBuilder]. Before calling any resource builder it constructs a generic [ResourceBuilderContext]. This context object
 * will be extended with the created resources as the builder finish creating them. The resources are grouped by private id.
 *
 *
 * Compute resource can be rolled back based on the different failure policies configured. Network resource failure immediately results in a failing deployment.
 */
abstract class AbstractResourceConnector : ResourceConnector {

    @Inject
    private val networkResourceService: NetworkResourceService? = null
    @Inject
    private val computeResourceService: ComputeResourceService? = null
    @Inject
    private val contextBuilders: ContextBuilders? = null

    @Throws(Exception::class)
    override fun launch(auth: AuthenticatedContext, stack: CloudStack, notifier: PersistenceNotifier,
                        adjustmentType: AdjustmentType, threshold: Long?): List<CloudResourceStatus> {
        val cloudContext = auth.cloudContext
        val platform = cloudContext.platform

        //context
        val context = contextBuilders!!.get(platform).contextInit(cloudContext, auth, stack.network, null, true)

        //network
        val networkStatuses = networkResourceService!!.buildResources(context, auth, stack.network, stack.security)
        context.addNetworkResources(getCloudResources(networkStatuses))

        //compute
        val computeStatuses = computeResourceService!!.buildResourcesForLaunch(context, auth, stack.groups, stack.image,
                adjustmentType, threshold)

        networkStatuses.addAll(computeStatuses)
        return networkStatuses
    }

    @Throws(Exception::class)
    override fun terminate(auth: AuthenticatedContext, stack: CloudStack, cloudResources: List<CloudResource>): List<CloudResourceStatus> {
        val cloudContext = auth.cloudContext
        val platform = cloudContext.platform

        //context
        val context = contextBuilders!!.get(platform).contextInit(cloudContext, auth, stack.network, cloudResources, false)

        //compute
        val computeStatuses = computeResourceService!!.deleteResources(context, auth, cloudResources, false)

        //network
        val networkStatuses = networkResourceService!!.deleteResources(context, auth, cloudResources, stack.network, false)

        computeStatuses.addAll(networkStatuses)
        return computeStatuses
    }

    @Throws(Exception::class)
    override fun upscale(auth: AuthenticatedContext, stack: CloudStack, resources: List<CloudResource>): List<CloudResourceStatus> {
        val cloudContext = auth.cloudContext
        val platform = cloudContext.platform

        //context
        val context = contextBuilders!!.get(platform).contextInit(cloudContext, auth, stack.network, resources, true)

        //network
        context.addNetworkResources(networkResourceService!!.getNetworkResources(platform, resources))

        //compute
        val scalingGroup = getScalingGroup(getGroup(stack.groups, getGroupName(stack)))

        return computeResourceService!!.buildResourcesForUpscale(context, auth, listOf<Group>(scalingGroup), stack.image)
    }

    @Throws(Exception::class)
    override fun downscale(auth: AuthenticatedContext, stack: CloudStack,
                           resources: List<CloudResource>, vms: List<CloudInstance>): List<CloudResourceStatus> {
        val cloudContext = auth.cloudContext
        val platform = cloudContext.platform

        //context
        val context = contextBuilders!!.get(platform).contextInit(cloudContext, auth, stack.network, resources, false)

        //compute
        //TODO we should somehow group the corresponding resources together
        val deleteResources = getDeleteResources(resources, vms)
        return computeResourceService!!.deleteResources(context, auth, deleteResources, true)
    }

    @Throws(Exception::class)
    override fun update(auth: AuthenticatedContext, stack: CloudStack, resources: List<CloudResource>): List<CloudResourceStatus> {
        val cloudContext = auth.cloudContext
        val platform = cloudContext.platform

        //context
        val context = contextBuilders!!.get(platform).contextInit(cloudContext, auth, stack.network, resources, true)

        //network
        val networkResources = networkResourceService!!.getNetworkResources(platform, resources)
        return networkResourceService.update(context, auth, stack.network, stack.security, networkResources)
    }

    override fun check(authenticatedContext: AuthenticatedContext, resources: List<CloudResource>): List<CloudResourceStatus> {
        throw UnsupportedOperationException()
    }

    private fun getCloudResources(resourceStatuses: List<CloudResourceStatus>): List<CloudResource> {
        val resources = ArrayList<CloudResource>()
        for (status in resourceStatuses) {
            resources.add(status.cloudResource)
        }
        return resources
    }

    private fun getScalingGroup(scalingGroup: Group): Group {
        val instances = ArrayList(scalingGroup.instances)
        val iterator = instances.iterator()
        while (iterator.hasNext()) {
            if (InstanceStatus.CREATE_REQUESTED !== iterator.next().template!!.status) {
                iterator.remove()
            }
        }
        return Group(scalingGroup.name, scalingGroup.type, instances)
    }

    private fun getGroup(groups: List<Group>, groupName: String): Group {
        var resultGroup: Group? = null
        for (group in groups) {
            if (groupName.equals(group.name, ignoreCase = true)) {
                resultGroup = group
                break
            }
        }
        return resultGroup
    }

    private fun getGroupName(stack: CloudStack): String? {
        for (group in stack.groups) {
            for (instance in group.instances) {
                val instanceTemplate = instance.template
                if (InstanceStatus.CREATE_REQUESTED === instanceTemplate.status) {
                    return instanceTemplate.groupName
                }
            }
        }
        return null
    }

    private fun getDeleteResources(resources: List<CloudResource>, instances: List<CloudInstance>): List<CloudResource> {
        val result = ArrayList<CloudResource>()
        for (instance in instances) {
            val instanceId = instance.instanceId
            for (resource in resources) {
                if (instanceId.equals(resource.name, ignoreCase = true)) {
                    result.add(resource)
                }
            }
        }
        return result
    }
}
