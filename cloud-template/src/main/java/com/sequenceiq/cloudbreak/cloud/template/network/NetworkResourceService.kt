package com.sequenceiq.cloudbreak.cloud.template.network

import com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup.CANCELLED
import java.util.Arrays.asList

import java.util.ArrayList
import java.util.Arrays

import javax.inject.Inject

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext
import com.sequenceiq.cloudbreak.cloud.context.CloudContext
import com.sequenceiq.cloudbreak.cloud.model.CloudResource
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus
import com.sequenceiq.cloudbreak.cloud.model.Network
import com.sequenceiq.cloudbreak.cloud.model.Platform
import com.sequenceiq.cloudbreak.cloud.model.Security
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier
import com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup
import com.sequenceiq.cloudbreak.cloud.scheduler.SyncPollingScheduler
import com.sequenceiq.cloudbreak.cloud.store.InMemoryStateStore
import com.sequenceiq.cloudbreak.cloud.task.PollTask
import com.sequenceiq.cloudbreak.cloud.template.NetworkResourceBuilder
import com.sequenceiq.cloudbreak.cloud.template.ResourceNotNeededException
import com.sequenceiq.cloudbreak.cloud.template.context.ResourceBuilderContext
import com.sequenceiq.cloudbreak.cloud.template.init.ResourceBuilders
import com.sequenceiq.cloudbreak.cloud.template.task.ResourcePollTaskFactory
import com.sequenceiq.cloudbreak.common.type.CommonStatus
import com.sequenceiq.cloudbreak.common.type.ResourceType

@Service
class NetworkResourceService {

    @Inject
    private val resourceBuilders: ResourceBuilders? = null
    @Inject
    private val syncPollingScheduler: SyncPollingScheduler<List<CloudResourceStatus>>? = null
    @Inject
    private val statusCheckFactory: ResourcePollTaskFactory? = null
    @Inject
    private val resourceNotifier: PersistenceNotifier? = null

    @Throws(Exception::class)
    fun buildResources(context: ResourceBuilderContext,
                       auth: AuthenticatedContext, network: Network, security: Security): List<CloudResourceStatus> {
        val cloudContext = auth.cloudContext
        val results = ArrayList<CloudResourceStatus>()
        for (builder in resourceBuilders!!.network(cloudContext.platform)) {
            val pollGroup = InMemoryStateStore.getStack(auth.cloudContext.id)
            if (pollGroup != null && CANCELLED == pollGroup) {
                break
            }
            try {
                val buildableResource = builder.create(context, auth, network)
                createResource(auth, buildableResource)
                val resource = builder.build(context, auth, network, security, buildableResource)
                updateResource(auth, resource)
                val task = statusCheckFactory!!.newPollResourceTask(builder, auth, asList(resource), context, true)
                val pollerResult = syncPollingScheduler!!.schedule(task)
                results.addAll(pollerResult)
            } catch (e: ResourceNotNeededException) {
                LOGGER.warn("Skipping resource creation: {}", e.message)
            }

        }
        return results
    }

    @Throws(Exception::class)
    fun deleteResources(context: ResourceBuilderContext,
                        auth: AuthenticatedContext, resources: List<CloudResource>, network: Network, cancellable: Boolean): List<CloudResourceStatus> {
        val cloudContext = auth.cloudContext
        val results = ArrayList<CloudResourceStatus>()
        val builderChain = resourceBuilders!!.network(cloudContext.platform)
        for (i in builderChain.indices.reversed()) {
            val builder = builderChain[i]
            val specificResources = getResources(resources, builder.resourceType())
            for (resource in specificResources) {
                if (resource.status === CommonStatus.CREATED) {
                    val deletedResource = builder.delete(context, auth, resource, network)
                    if (deletedResource != null) {
                        val task = statusCheckFactory!!.newPollResourceTask(
                                builder, auth, asList(deletedResource), context, cancellable)
                        val pollerResult = syncPollingScheduler!!.schedule(task)
                        results.addAll(pollerResult)
                    }
                }
                resourceNotifier!!.notifyDeletion(resource, cloudContext)
            }
        }
        return results
    }

    @Throws(Exception::class)
    fun update(context: ResourceBuilderContext, auth: AuthenticatedContext,
               network: Network, security: Security, networkResources: List<CloudResource>): List<CloudResourceStatus> {
        val results = ArrayList<CloudResourceStatus>()
        val cloudContext = auth.cloudContext
        for (builder in resourceBuilders!!.network(cloudContext.platform)) {
            val resource = getResources(networkResources, builder.resourceType())[0]
            val status = builder.update(context, auth, network, security, resource)
            if (status != null) {
                val task = statusCheckFactory!!.newPollResourceTask(
                        builder, auth, asList(status.cloudResource), context, true)
                val pollerResult = syncPollingScheduler!!.schedule(task)
                results.addAll(pollerResult)
            }
        }
        return results
    }

    fun getNetworkResources(platform: Platform, resources: List<CloudResource>): List<CloudResource> {
        val types = ArrayList<ResourceType>()
        for (builder in resourceBuilders!!.network(platform)) {
            types.add(builder.resourceType())
        }
        return getResources(resources, types)
    }

    @Throws(Exception::class)
    protected fun createResource(auth: AuthenticatedContext, buildableResource: CloudResource): CloudResource {
        if (buildableResource.isPersistent) {
            resourceNotifier!!.notifyAllocation(buildableResource, auth.cloudContext)
        }
        return buildableResource
    }

    @Throws(Exception::class)
    protected fun updateResource(auth: AuthenticatedContext, buildableResource: CloudResource): CloudResource {
        if (buildableResource.isPersistent) {
            resourceNotifier!!.notifyUpdate(buildableResource, auth.cloudContext)
        }
        return buildableResource
    }

    private fun getResources(resources: List<CloudResource>, type: ResourceType): List<CloudResource> {
        return getResources(resources, Arrays.asList(type))
    }

    private fun getResources(resources: List<CloudResource>, types: List<ResourceType>): List<CloudResource> {
        val filtered = ArrayList<CloudResource>()
        for (resource in resources) {
            if (types.contains(resource.type)) {
                filtered.add(resource)
            }
        }
        return filtered
    }

    companion object {

        private val LOGGER = LoggerFactory.getLogger(NetworkResourceService::class.java)
    }
}
