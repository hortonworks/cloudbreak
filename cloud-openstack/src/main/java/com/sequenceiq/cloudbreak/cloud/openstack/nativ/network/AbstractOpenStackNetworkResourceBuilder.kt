package com.sequenceiq.cloudbreak.cloud.openstack.nativ.network

import java.util.Collections

import javax.inject.Inject

import com.google.common.collect.Maps
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext
import com.sequenceiq.cloudbreak.cloud.model.CloudResource
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus
import com.sequenceiq.cloudbreak.cloud.model.Network
import com.sequenceiq.cloudbreak.cloud.model.Security
import com.sequenceiq.cloudbreak.cloud.openstack.auth.OpenStackClient
import com.sequenceiq.cloudbreak.cloud.openstack.nativ.AbstractOpenStackResourceBuilder
import com.sequenceiq.cloudbreak.cloud.openstack.nativ.OpenStackResourceException
import com.sequenceiq.cloudbreak.cloud.openstack.nativ.context.OpenStackContext
import com.sequenceiq.cloudbreak.cloud.openstack.nativ.service.OpenStackResourceNameService
import com.sequenceiq.cloudbreak.cloud.template.NetworkResourceBuilder

abstract class AbstractOpenStackNetworkResourceBuilder : AbstractOpenStackResourceBuilder(), NetworkResourceBuilder<OpenStackContext> {

    @Inject
    private val resourceNameService: OpenStackResourceNameService? = null
    @Inject
    private val openStackClient: OpenStackClient? = null

    override fun create(context: OpenStackContext, auth: AuthenticatedContext, network: Network): CloudResource {
        val resourceName = resourceNameService!!.resourceName(resourceType(), context.name)
        return createNamedResource(resourceType(), resourceName)
    }

    @Throws(Exception::class)
    override fun update(context: OpenStackContext, auth: AuthenticatedContext, network: Network, security: Security, resource: CloudResource): CloudResourceStatus? {
        return null
    }

    override fun order(): Int {
        val order = ORDER[javaClass] ?: throw OpenStackResourceException(String.format("No resource order found for class: %s", javaClass))
        return order
    }

    override fun checkResources(context: OpenStackContext, auth: AuthenticatedContext, resources: List<CloudResource>): List<CloudResourceStatus> {
        return checkResources(resourceType(), context, auth, resources)
    }

    companion object {

        private val ORDER: Map<Class<out AbstractOpenStackNetworkResourceBuilder>, Int>

        init {
            val map = Maps.newHashMap<Class<out AbstractOpenStackNetworkResourceBuilder>, Int>()
            map.put(OpenStackNetworkResourceBuilder::class.java, 0)
            map.put(OpenStackSecurityGroupResourceBuilder::class.java, 0)
            map.put(OpenStackSubnetResourceBuilder::class.java, 1)
            map.put(OpenStackRouterResourceBuilder::class.java, 2)
            ORDER = Collections.unmodifiableMap(map)
        }
    }
}
