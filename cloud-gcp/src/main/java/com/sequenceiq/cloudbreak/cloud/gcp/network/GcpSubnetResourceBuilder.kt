package com.sequenceiq.cloudbreak.cloud.gcp.network

import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil.getSubnetId
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil.isExistingSubnet
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil.legacyNetwork
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil.newNetworkAndSubnet
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil.newSubnetInExistingNetwork
import com.sequenceiq.cloudbreak.common.type.ResourceType.GCP_SUBNET

import org.springframework.stereotype.Service

import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.services.compute.Compute
import com.google.api.services.compute.model.Operation
import com.google.api.services.compute.model.Subnetwork
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext
import com.sequenceiq.cloudbreak.cloud.gcp.GcpResourceException
import com.sequenceiq.cloudbreak.cloud.gcp.context.GcpContext
import com.sequenceiq.cloudbreak.cloud.model.CloudResource
import com.sequenceiq.cloudbreak.cloud.model.Network
import com.sequenceiq.cloudbreak.cloud.model.Security
import com.sequenceiq.cloudbreak.cloud.template.ResourceNotNeededException
import com.sequenceiq.cloudbreak.common.type.ResourceType

@Service
class GcpSubnetResourceBuilder : AbstractGcpNetworkBuilder() {

    override fun create(context: GcpContext, auth: AuthenticatedContext, network: Network): CloudResource {
        if (legacyNetwork(network)) {
            throw ResourceNotNeededException("Legacy GCP networks doesn't support subnets. Subnet won't be created.")
        }
        val resourceName = if (isExistingSubnet(network)) getSubnetId(network) else resourceNameService.resourceName(resourceType(), context.name)
        return createNamedResource(resourceType(), resourceName)
    }

    @Throws(Exception::class)
    override fun build(context: GcpContext, auth: AuthenticatedContext, network: Network, security: Security, resource: CloudResource): CloudResource {
        if (newNetworkAndSubnet(network) || newSubnetInExistingNetwork(network)) {
            val compute = context.compute
            val projectId = context.projectId

            val gcpSubnet = Subnetwork()
            gcpSubnet.name = resource.name
            gcpSubnet.ipCidrRange = network.subnet.cidr

            val networkName = context.getStringParameter(GcpNetworkResourceBuilder.NETWORK_NAME)
            gcpSubnet.network = String.format("https://www.googleapis.com/compute/v1/projects/%s/global/networks/%s", projectId, networkName)

            val snInsert = compute.subnetworks().insert(projectId, auth.cloudContext.location!!.region.value(), gcpSubnet)
            try {
                val operation = snInsert.execute()
                if (operation.httpErrorStatusCode != null) {
                    throw GcpResourceException(operation.httpErrorMessage, resourceType(), resource.name)
                }
                context.putParameter(SUBNET_NAME, resource.name)
                return createOperationAwareCloudResource(resource, operation)
            } catch (e: GoogleJsonResponseException) {
                throw GcpResourceException(checkException(e), resourceType(), resource.name)
            }

        }
        context.putParameter(SUBNET_NAME, resource.name)
        return CloudResource.Builder().cloudResource(resource).persistent(false).build()
    }

    @Throws(Exception::class)
    override fun delete(context: GcpContext, auth: AuthenticatedContext, resource: CloudResource, network: Network): CloudResource? {
        if (newNetworkAndSubnet(network) || newSubnetInExistingNetwork(network)) {
            val compute = context.compute
            val projectId = context.projectId
            try {
                val operation = compute.subnetworks().delete(projectId, context.location.region.value(), resource.name).execute()
                return createOperationAwareCloudResource(resource, operation)
            } catch (e: GoogleJsonResponseException) {
                exceptionHandler(e, resource.name, resourceType())
                return null
            }

        }
        return null
    }

    override fun resourceType(): ResourceType {
        return GCP_SUBNET
    }

    override fun order(): Int {
        return 1
    }

    companion object {

        val SUBNET_NAME = "subnetName"
    }
}
