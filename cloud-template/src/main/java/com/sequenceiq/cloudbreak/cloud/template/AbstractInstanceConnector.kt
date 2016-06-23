package com.sequenceiq.cloudbreak.cloud.template

import javax.inject.Inject

import com.sequenceiq.cloudbreak.cloud.InstanceConnector
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext
import com.sequenceiq.cloudbreak.cloud.context.CloudContext
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance
import com.sequenceiq.cloudbreak.cloud.model.CloudResource
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus
import com.sequenceiq.cloudbreak.cloud.model.Platform
import com.sequenceiq.cloudbreak.cloud.template.compute.ComputeResourceService
import com.sequenceiq.cloudbreak.cloud.template.context.ResourceBuilderContext
import com.sequenceiq.cloudbreak.cloud.template.init.ContextBuilders

/**
 * Abstract base implementation of [InstanceConnector] for cloud provider which do not have template based deployments. It provides
 * the functionality to call the resource builders in order to stop and start the resources. **Only compute** resources can be stopped/started.
 */
abstract class AbstractInstanceConnector : InstanceConnector {

    @Inject
    private val computeResourceService: ComputeResourceService? = null
    @Inject
    private val contextBuilders: ContextBuilders? = null

    @Throws(Exception::class)
    override fun stop(ac: AuthenticatedContext, resources: List<CloudResource>, vms: List<CloudInstance>): List<CloudVmInstanceStatus> {
        val cloudContext = ac.cloudContext
        val platform = cloudContext.platform

        //context
        val context = contextBuilders!!.get(platform).contextInit(cloudContext, ac, null, resources, false)

        //compute
        return computeResourceService!!.stopInstances(context, ac, resources, vms)
    }

    @Throws(Exception::class)
    override fun start(ac: AuthenticatedContext, resources: List<CloudResource>, vms: List<CloudInstance>): List<CloudVmInstanceStatus> {
        val cloudContext = ac.cloudContext
        val platform = cloudContext.platform

        //context
        val context = contextBuilders!!.get(platform).contextInit(cloudContext, ac, null, resources, true)

        //compute
        return computeResourceService!!.startInstances(context, ac, resources, vms)
    }

}
