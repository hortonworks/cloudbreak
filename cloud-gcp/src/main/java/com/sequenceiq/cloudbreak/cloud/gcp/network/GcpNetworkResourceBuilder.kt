package com.sequenceiq.cloudbreak.cloud.gcp.network

import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil.getCustomNetworkId
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil.isExistingNetwork

import org.springframework.stereotype.Service

import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.services.compute.Compute
import com.google.api.services.compute.model.Operation
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext
import com.sequenceiq.cloudbreak.cloud.gcp.GcpResourceException
import com.sequenceiq.cloudbreak.cloud.gcp.context.GcpContext
import com.sequenceiq.cloudbreak.cloud.model.CloudResource
import com.sequenceiq.cloudbreak.cloud.model.Network
import com.sequenceiq.cloudbreak.cloud.model.Security
import com.sequenceiq.cloudbreak.common.type.ResourceType

@Service
class GcpNetworkResourceBuilder : AbstractGcpNetworkBuilder() {

    override fun create(context: GcpContext, auth: AuthenticatedContext, network: Network): CloudResource {
        val name = if (isExistingNetwork(network)) getCustomNetworkId(network) else resourceNameService.resourceName(resourceType(), context.name)
        return createNamedResource(resourceType(), name)
    }

    @Throws(Exception::class)
    override fun build(context: GcpContext, auth: AuthenticatedContext, network: Network, security: Security, resource: CloudResource): CloudResource {
        if (!isExistingNetwork(network)) {
            val compute = context.compute
            val projectId = context.projectId

            val gcpNetwork = com.google.api.services.compute.model.Network()
            gcpNetwork.name = resource.name
            gcpNetwork.autoCreateSubnetworks = false
            val networkInsert = compute.networks().insert(projectId, gcpNetwork)
            try {
                val operation = networkInsert.execute()
                if (operation.httpErrorStatusCode != null) {
                    throw GcpResourceException(operation.httpErrorMessage, resourceType(), resource.name)
                }
                context.putParameter(NETWORK_NAME, resource.name)
                return createOperationAwareCloudResource(resource, operation)
            } catch (e: GoogleJsonResponseException) {
                throw GcpResourceException(checkException(e), resourceType(), resource.name)
            }

        }
        context.putParameter(NETWORK_NAME, resource.name)
        return CloudResource.Builder().cloudResource(resource).persistent(false).build()
    }

    @Throws(Exception::class)
    override fun delete(context: GcpContext, auth: AuthenticatedContext, resource: CloudResource, network: Network): CloudResource? {
        if (!isExistingNetwork(network)) {
            val compute = context.compute
            val projectId = context.projectId
            try {
                val operation = compute.networks().delete(projectId, resource.name).execute()
                return createOperationAwareCloudResource(resource, operation)
            } catch (e: GoogleJsonResponseException) {
                exceptionHandler(e, resource.name, resourceType())
                return null
            }

        }
        return null
    }

    override fun resourceType(): ResourceType {
        return ResourceType.GCP_NETWORK
    }

    override fun order(): Int {
        return 0
    }

    companion object {

        val NETWORK_NAME = "netName"
    }
}
