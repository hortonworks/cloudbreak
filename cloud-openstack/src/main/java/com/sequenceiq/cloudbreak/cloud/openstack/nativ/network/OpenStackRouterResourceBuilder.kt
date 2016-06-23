package com.sequenceiq.cloudbreak.cloud.openstack.nativ.network

import javax.inject.Inject

import org.openstack4j.api.Builders
import org.openstack4j.api.OSClient
import org.openstack4j.api.exceptions.OS4JException
import org.openstack4j.model.compute.ActionResponse
import org.openstack4j.model.network.AttachInterfaceType
import org.openstack4j.model.network.Router
import org.openstack4j.model.network.State
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
import com.sequenceiq.cloudbreak.cloud.openstack.view.NeutronNetworkView
import com.sequenceiq.cloudbreak.cloud.template.ResourceNotNeededException
import com.sequenceiq.cloudbreak.common.type.ResourceType

@Service
class OpenStackRouterResourceBuilder : AbstractOpenStackNetworkResourceBuilder() {

    @Inject
    private val utils: OpenStackUtils? = null

    override fun create(context: OpenStackContext, auth: AuthenticatedContext, network: Network): CloudResource {
        if (utils!!.isExistingSubnet(network)) {
            throw ResourceNotNeededException("Router isn't needed when a subnet is reused.")
        }
        return super.create(context, auth, network)
    }

    @Throws(Exception::class)
    override fun build(context: OpenStackContext, auth: AuthenticatedContext, network: Network, security: Security, resource: CloudResource): CloudResource {
        try {
            val osClient = createOSClient(auth)
            val networkView = NeutronNetworkView(network)
            var routerId = utils!!.getCustomRouterId(network)
            if (!utils.isExistingNetwork(network)) {
                val router = Builders.router().name(resource.name).adminStateUp(true).tenantId(context.getStringParameter(OpenStackConstants.TENANT_ID)).externalGateway(networkView.publicNetId).build()
                val newRouter = osClient.networking().router().create(router) ?: throw OpenStackResourceException("Router creation failed, maybe network does not exists", resourceType(), resource.name)
                routerId = newRouter.id
            }
            if (!utils.isExistingSubnet(network)) {
                osClient.networking().router().attachInterface(routerId, AttachInterfaceType.SUBNET, context.getStringParameter(OpenStackConstants.SUBNET_ID))
            }
            return createPersistedResource(resource, routerId)
        } catch (ex: OS4JException) {
            throw OpenStackResourceException("Router creation failed", resourceType(), resource.name, ex)
        }

    }

    @Throws(Exception::class)
    override fun delete(context: OpenStackContext, auth: AuthenticatedContext, resource: CloudResource, network: Network): CloudResource? {
        try {
            val osClient = createOSClient(auth)
            val subnetId = context.getStringParameter(OpenStackConstants.SUBNET_ID)
            if (!utils!!.isExistingSubnet(network)) {
                osClient.networking().router().detachInterface(resource.reference, subnetId, null)
            }
            if (!utils.isExistingNetwork(network)) {
                val response = osClient.networking().router().delete(resource.reference)
                return checkDeleteResponse(response, resourceType(), auth, resource, "Router deletion failed")
            }
            return null
        } catch (ex: OS4JException) {
            throw OpenStackResourceException("Router deletion failed", resourceType(), resource.name, ex)
        }

    }

    override fun resourceType(): ResourceType {
        return ResourceType.OPENSTACK_ROUTER
    }

    override fun checkStatus(context: OpenStackContext, auth: AuthenticatedContext, resource: CloudResource): Boolean {
        val cloudContext = auth.cloudContext
        val osClient = createOSClient(auth)
        val osRouter = osClient.networking().router().get(resource.reference)
        if (osRouter != null && context.isBuild) {
            val routerStatus = osRouter.status
            if (State.ERROR == routerStatus) {
                throw OpenStackResourceException("Router in failed state", resource.type, cloudContext.name, cloudContext.id,
                        resource.name)
            }
            return routerStatus == State.ACTIVE
        } else if (osRouter == null && !context.isBuild) {
            return true
        }
        return false
    }
}
