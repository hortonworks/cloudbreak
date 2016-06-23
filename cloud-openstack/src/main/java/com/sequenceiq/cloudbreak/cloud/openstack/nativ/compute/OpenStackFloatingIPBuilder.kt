package com.sequenceiq.cloudbreak.cloud.openstack.nativ.compute

import java.util.Collections

import org.openstack4j.api.OSClient
import org.openstack4j.api.exceptions.OS4JException
import org.openstack4j.model.compute.ActionResponse
import org.openstack4j.model.compute.FloatingIP
import org.openstack4j.model.compute.Server
import org.springframework.stereotype.Service

import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext
import com.sequenceiq.cloudbreak.cloud.model.CloudResource
import com.sequenceiq.cloudbreak.cloud.model.Group
import com.sequenceiq.cloudbreak.cloud.model.Image
import com.sequenceiq.cloudbreak.cloud.openstack.common.OpenStackConstants
import com.sequenceiq.cloudbreak.cloud.openstack.nativ.OpenStackResourceException
import com.sequenceiq.cloudbreak.cloud.openstack.nativ.context.OpenStackContext
import com.sequenceiq.cloudbreak.common.type.ResourceType

@Service
class OpenStackFloatingIPBuilder : AbstractOpenStackComputeResourceBuilder() {
    @Throws(Exception::class)
    override fun build(context: OpenStackContext, privateId: Long, auth: AuthenticatedContext, group: Group, image: Image,
                       buildableResource: List<CloudResource>): List<CloudResource> {
        val resource = buildableResource[0]
        try {
            val publicNetId = context.getStringParameter(OpenStackConstants.PUBLIC_NET_ID)
            if (publicNetId != null) {
                val osClient = createOSClient(auth)
                val computeResources = context.getComputeResources(privateId)
                val instance = getInstance(computeResources)
                val unusedIp = osClient.compute().floatingIps().allocateIP(publicNetId)
                val response = osClient.compute().floatingIps().addFloatingIP(instance.getParameter<Server>(OpenStackConstants.SERVER, Server::class.java),
                        unusedIp.floatingIpAddress)
                if (!response.isSuccess) {
                    throw OpenStackResourceException("Add floating-ip to server failed", resourceType(), resource.name,
                            auth.cloudContext.id, response.fault)
                }
                return listOf<CloudResource>(createPersistedResource(resource, unusedIp.id))
            }
            return Collections.EMPTY_LIST as List<CloudResource>
        } catch (ex: OS4JException) {
            throw OpenStackResourceException("Add floating-ip to server failed", resourceType(), resource.name, ex)
        }

    }

    @SuppressWarnings("unchecked")
    @Throws(Exception::class)
    override fun delete(context: OpenStackContext, auth: AuthenticatedContext, resource: CloudResource): CloudResource? {
        context.getParameter<List<Any>>(OpenStackConstants.FLOATING_IP_IDS, List<Any>::class.java).add(resource.reference)
        return null
    }

    override fun resourceType(): ResourceType {
        return ResourceType.OPENSTACK_FLOATING_IP
    }

    override fun checkStatus(context: OpenStackContext, auth: AuthenticatedContext, resource: CloudResource): Boolean {
        return true
    }

    private fun getInstance(computeResources: List<CloudResource>): CloudResource {
        var instance: CloudResource? = null
        for (computeResource in computeResources) {
            if (computeResource.type === ResourceType.OPENSTACK_INSTANCE) {
                instance = computeResource
            }
        }
        return instance
    }
}
