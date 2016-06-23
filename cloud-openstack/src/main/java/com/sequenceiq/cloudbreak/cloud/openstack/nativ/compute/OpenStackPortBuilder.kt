package com.sequenceiq.cloudbreak.cloud.openstack.nativ.compute

import java.util.Collections

import org.openstack4j.api.Builders
import org.openstack4j.api.OSClient
import org.openstack4j.api.exceptions.OS4JException
import org.openstack4j.model.compute.ActionResponse
import org.openstack4j.model.network.Port
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
class OpenStackPortBuilder : AbstractOpenStackComputeResourceBuilder() {
    @Throws(Exception::class)
    override fun build(context: OpenStackContext, privateId: Long, auth: AuthenticatedContext, group: Group, image: Image,
                       buildableResource: List<CloudResource>): List<CloudResource> {
        val resource = buildableResource[0]
        try {
            val osClient = createOSClient(auth)
            var port = Builders.port().tenantId(context.getStringParameter(OpenStackConstants.TENANT_ID)).networkId(context.getStringParameter(OpenStackConstants.NETWORK_ID)).fixedIp(null, context.getStringParameter(OpenStackConstants.SUBNET_ID)).securityGroup(context.getStringParameter(OpenStackConstants.SECURITYGROUP_ID)).build()
            port = osClient.networking().port().create(port)
            return listOf<CloudResource>(createPersistedResource(resource, port.id, Collections.singletonMap<String, Any>(
                    OpenStackConstants.PORT_ID, port.id)))
        } catch (ex: OS4JException) {
            throw OpenStackResourceException("Port creation failed", resourceType(), resource.name, ex)
        }

    }

    @Throws(Exception::class)
    override fun delete(context: OpenStackContext, auth: AuthenticatedContext, resource: CloudResource): CloudResource {
        try {
            val osClient = createOSClient(auth)
            val response = osClient.networking().port().delete(resource.reference)
            return checkDeleteResponse(response, resourceType(), auth, resource, "Port deletion failed")
        } catch (ex: OS4JException) {
            throw OpenStackResourceException("Port deletion failed", resourceType(), resource.name, ex)
        }

    }

    override fun resourceType(): ResourceType {
        return ResourceType.OPENSTACK_PORT
    }

    override fun checkStatus(context: OpenStackContext, auth: AuthenticatedContext, resource: CloudResource): Boolean {
        return true
    }
}
