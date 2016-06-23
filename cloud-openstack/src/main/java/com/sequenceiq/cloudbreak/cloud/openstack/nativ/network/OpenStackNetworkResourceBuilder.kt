package com.sequenceiq.cloudbreak.cloud.openstack.nativ.network

import com.sequenceiq.cloudbreak.cloud.openstack.common.OpenStackConstants.NETWORK_ID

import javax.inject.Inject

import org.openstack4j.api.Builders
import org.openstack4j.api.OSClient
import org.openstack4j.api.exceptions.OS4JException
import org.openstack4j.model.compute.ActionResponse
import org.openstack4j.model.network.State
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext
import com.sequenceiq.cloudbreak.cloud.context.CloudContext
import com.sequenceiq.cloudbreak.cloud.model.CloudResource
import com.sequenceiq.cloudbreak.cloud.model.Network
import com.sequenceiq.cloudbreak.cloud.model.Security
import com.sequenceiq.cloudbreak.cloud.openstack.common.OpenStackConstants
import com.sequenceiq.cloudbreak.cloud.openstack.common.OpenStackUtils
import com.sequenceiq.cloudbreak.cloud.openstack.nativ.OpenStackResourceException
import com.sequenceiq.cloudbreak.cloud.openstack.nativ.context.OpenStackContext
import com.sequenceiq.cloudbreak.common.type.ResourceType

@Service
class OpenStackNetworkResourceBuilder : AbstractOpenStackNetworkResourceBuilder() {

    @Inject
    private val utils: OpenStackUtils? = null

    @Throws(Exception::class)
    override fun build(context: OpenStackContext, auth: AuthenticatedContext, network: Network, security: Security, buildableResource: CloudResource): CloudResource {
        val osClient = createOSClient(auth)
        try {
            var networkId = if (utils!!.isExistingNetwork(network)) utils.getCustomNetworkId(network) else context.getParameter<String>(NETWORK_ID, String::class.java)
            if (!utils.isExistingNetwork(network)) {
                val osNetwork = Builders.network().name(buildableResource.name).tenantId(context.getStringParameter(OpenStackConstants.TENANT_ID)).adminStateUp(true).build()
                networkId = osClient.networking().network().create(osNetwork).id
            }
            context.putParameter(OpenStackConstants.NETWORK_ID, networkId)
            return createPersistedResource(buildableResource, networkId)
        } catch (ex: OS4JException) {
            throw OpenStackResourceException("Network creation failed", resourceType(), buildableResource.name, ex)
        }

    }

    @Throws(Exception::class)
    override fun delete(context: OpenStackContext, auth: AuthenticatedContext, resource: CloudResource, network: Network): CloudResource? {
        try {
            val osClient = createOSClient(auth)
            deAllocateFloatingIps(context, osClient)
            if (!utils!!.isExistingNetwork(network)) {
                val response = osClient.networking().network().delete(resource.reference)
                return checkDeleteResponse(response, resourceType(), auth, resource, "Network deletion failed")
            }
            return null
        } catch (ex: OS4JException) {
            throw OpenStackResourceException("Network deletion failed", resourceType(), resource.name, ex)
        }

    }

    override fun resourceType(): ResourceType {
        return ResourceType.OPENSTACK_NETWORK
    }

    override fun checkStatus(context: OpenStackContext, auth: AuthenticatedContext, resource: CloudResource): Boolean {
        val cloudContext = auth.cloudContext
        val osClient = createOSClient(auth)
        val osNetwork = osClient.networking().network().get(resource.reference)
        if (osNetwork != null && context.isBuild) {
            val networkStatus = osNetwork.status
            if (State.ERROR == networkStatus) {
                throw OpenStackResourceException("Network in failed state", resource.type, resource.name, cloudContext.id,
                        networkStatus.name)
            }
            return networkStatus == State.ACTIVE
        } else if (osNetwork == null && !context.isBuild) {
            return true
        }
        return false
    }

    @SuppressWarnings("unchecked")
    private fun deAllocateFloatingIps(context: OpenStackContext, osClient: OSClient) {
        val floatingIpIds = context.getParameter<List<Any>>(OpenStackConstants.FLOATING_IP_IDS, List<Any>::class.java)
        for (floatingIpId in floatingIpIds) {
            try {
                val response = osClient.compute().floatingIps().deallocateIP(floatingIpId)
                if (!response.isSuccess) {
                    LOGGER.warn("FloatingIp {} cannot be deallocated: {}", floatingIpId, response.fault)
                }
            } catch (ex: OS4JException) {
                LOGGER.warn("FloatingIp {} cannot be deallocated: {}", floatingIpId, ex.message)
            }

        }
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(OpenStackNetworkResourceBuilder::class.java)
    }
}
