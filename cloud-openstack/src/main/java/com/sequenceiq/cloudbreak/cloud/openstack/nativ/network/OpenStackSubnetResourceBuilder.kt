package com.sequenceiq.cloudbreak.cloud.openstack.nativ.network

import com.sequenceiq.cloudbreak.cloud.openstack.common.OpenStackConstants.SUBNET_ID

import javax.inject.Inject

import org.openstack4j.api.Builders
import org.openstack4j.api.OSClient
import org.openstack4j.api.exceptions.OS4JException
import org.openstack4j.model.compute.ActionResponse
import org.openstack4j.model.network.IPVersionType
import org.openstack4j.model.network.Subnet
import org.springframework.stereotype.Service

import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext
import com.sequenceiq.cloudbreak.cloud.model.CloudResource
import com.sequenceiq.cloudbreak.cloud.model.Network
import com.sequenceiq.cloudbreak.cloud.model.Security
import com.sequenceiq.cloudbreak.cloud.openstack.common.OpenStackConstants
import com.sequenceiq.cloudbreak.cloud.openstack.common.OpenStackUtils
import com.sequenceiq.cloudbreak.cloud.openstack.nativ.OpenStackResourceException
import com.sequenceiq.cloudbreak.cloud.openstack.nativ.context.OpenStackContext
import com.sequenceiq.cloudbreak.cloud.openstack.view.NeutronNetworkView
import com.sequenceiq.cloudbreak.common.type.ResourceType

@Service
class OpenStackSubnetResourceBuilder : AbstractOpenStackNetworkResourceBuilder() {

    @Inject
    private val utils: OpenStackUtils? = null

    @Throws(Exception::class)
    override fun build(context: OpenStackContext, auth: AuthenticatedContext, network: Network, security: Security, resource: CloudResource): CloudResource {
        try {
            var subnetId = if (utils!!.isExistingSubnet(network)) utils.getCustomSubnetId(network) else context.getParameter<String>(SUBNET_ID, String::class.java)
            if (!utils.isExistingSubnet(network)) {
                val osClient = createOSClient(auth)
                val networkView = NeutronNetworkView(network)
                val subnet = Builders.subnet().name(resource.name).networkId(context.getParameter<String>(OpenStackConstants.NETWORK_ID, String::class.java)).tenantId(context.getStringParameter(OpenStackConstants.TENANT_ID)).ipVersion(IPVersionType.V4).cidr(networkView.subnetCIDR).enableDHCP(true).build()
                subnetId = osClient.networking().subnet().create(subnet).id
            }
            context.putParameter(OpenStackConstants.SUBNET_ID, subnetId)
            return createPersistedResource(resource, subnetId)
        } catch (ex: OS4JException) {
            throw OpenStackResourceException("Subnet creation failed", resourceType(), resource.name, ex)
        }

    }

    @Throws(Exception::class)
    override fun delete(context: OpenStackContext, auth: AuthenticatedContext, resource: CloudResource, network: Network): CloudResource? {
        try {
            if (!utils!!.isExistingSubnet(network)) {
                val osClient = createOSClient(auth)
                val response = osClient.networking().subnet().delete(resource.reference)
                return checkDeleteResponse(response, resourceType(), auth, resource, "Subnet deletion failed")
            }
            return null
        } catch (ex: OS4JException) {
            throw OpenStackResourceException("Subnet deletion failed", resourceType(), resource.name, ex)
        }

    }

    override fun resourceType(): ResourceType {
        return ResourceType.OPENSTACK_SUBNET
    }

    override fun checkStatus(context: OpenStackContext, auth: AuthenticatedContext, resource: CloudResource): Boolean {
        return true
    }
}
