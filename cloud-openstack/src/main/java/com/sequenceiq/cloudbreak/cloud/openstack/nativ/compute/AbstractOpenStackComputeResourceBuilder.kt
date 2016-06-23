package com.sequenceiq.cloudbreak.cloud.openstack.nativ.compute

import java.util.Arrays
import java.util.Collections

import javax.inject.Inject

import com.google.common.collect.Maps
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext
import com.sequenceiq.cloudbreak.cloud.context.CloudContext
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance
import com.sequenceiq.cloudbreak.cloud.model.CloudResource
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus
import com.sequenceiq.cloudbreak.cloud.model.Group
import com.sequenceiq.cloudbreak.cloud.model.Image
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate
import com.sequenceiq.cloudbreak.cloud.openstack.nativ.AbstractOpenStackResourceBuilder
import com.sequenceiq.cloudbreak.cloud.openstack.nativ.OpenStackResourceException
import com.sequenceiq.cloudbreak.cloud.openstack.nativ.context.OpenStackContext
import com.sequenceiq.cloudbreak.cloud.openstack.nativ.service.OpenStackResourceNameService
import com.sequenceiq.cloudbreak.cloud.template.ComputeResourceBuilder

abstract class AbstractOpenStackComputeResourceBuilder : AbstractOpenStackResourceBuilder(), ComputeResourceBuilder<OpenStackContext> {

    @Inject
    private val resourceNameService: OpenStackResourceNameService? = null

    override fun create(context: OpenStackContext, privateId: Long, auth: AuthenticatedContext, group: Group, image: Image): List<CloudResource> {
        val cloudContext = auth.cloudContext
        val resourceName = resourceNameService!!.resourceName(resourceType(), cloudContext.name, group.name, privateId)
        return Arrays.asList(createNamedResource(resourceType(), resourceName))
    }

    override fun checkInstances(context: OpenStackContext, auth: AuthenticatedContext, instances: List<CloudInstance>): List<CloudVmInstanceStatus>? {
        return null
    }

    override fun start(context: OpenStackContext, auth: AuthenticatedContext, instance: CloudInstance): CloudVmInstanceStatus? {
        return null
    }

    override fun stop(context: OpenStackContext, auth: AuthenticatedContext, instance: CloudInstance): CloudVmInstanceStatus? {
        return null
    }

    override fun checkResources(context: OpenStackContext, auth: AuthenticatedContext, resources: List<CloudResource>): List<CloudResourceStatus> {
        return checkResources(resourceType(), context, auth, resources)
    }

    override fun order(): Int {
        val order = ORDER[javaClass] ?: throw OpenStackResourceException(String.format("No resource order found for class: %s", javaClass))
        return order
    }

    protected fun getInstanceTemplate(group: Group, privateId: Long): InstanceTemplate? {
        for (instance in group.instances) {
            val template = instance.template
            if (template.privateId!!.toLong() == privateId) {
                return template
            }
        }
        return null
    }

    companion object {

        private val ORDER: Map<Class<out AbstractOpenStackComputeResourceBuilder>, Int>

        init {
            val map = Maps.newHashMap<Class<out AbstractOpenStackComputeResourceBuilder>, Int>()
            map.put(OpenStackPortBuilder::class.java, 0)
            map.put(OpenStackAttachedDiskResourceBuilder::class.java, 0)
            map.put(OpenStackInstanceBuilder::class.java, 1)
            map.put(OpenStackFloatingIPBuilder::class.java, 2)
            ORDER = Collections.unmodifiableMap(map)
        }
    }
}
