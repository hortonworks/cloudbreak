package com.sequenceiq.cloudbreak.cloud.gcp.network

import org.springframework.stereotype.Service

import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.services.compute.Compute
import com.google.api.services.compute.model.Address
import com.google.api.services.compute.model.Operation
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext
import com.sequenceiq.cloudbreak.cloud.gcp.GcpResourceException
import com.sequenceiq.cloudbreak.cloud.gcp.context.GcpContext
import com.sequenceiq.cloudbreak.cloud.model.CloudResource
import com.sequenceiq.cloudbreak.cloud.model.Network
import com.sequenceiq.cloudbreak.cloud.model.Security
import com.sequenceiq.cloudbreak.common.type.ResourceType

@Service
class GcpReservedIpResourceBuilder : AbstractGcpNetworkBuilder() {

    override fun create(context: GcpContext, auth: AuthenticatedContext, network: Network): CloudResource {
        val resourceName = resourceNameService.resourceName(resourceType(), context.name)
        return createNamedResource(resourceType(), resourceName)
    }

    @Throws(Exception::class)
    override fun build(context: GcpContext, auth: AuthenticatedContext, network: Network, security: Security, resource: CloudResource): CloudResource {
        val projectId = context.projectId
        val region = context.location.region.value()

        val address = Address()
        address.name = resource.name

        val networkInsert = context.compute.addresses().insert(projectId, region, address)
        try {
            val operation = networkInsert.execute()
            if (operation.httpErrorStatusCode != null) {
                throw GcpResourceException(operation.httpErrorMessage, resourceType(), resource.name)
            }
            return createOperationAwareCloudResource(resource, operation)
        } catch (e: GoogleJsonResponseException) {
            throw GcpResourceException(checkException(e), resourceType(), resource.name)
        }

    }

    @Throws(Exception::class)
    override fun delete(context: GcpContext, auth: AuthenticatedContext, resource: CloudResource, network: Network): CloudResource? {
        val compute = context.compute
        val projectId = context.projectId
        val region = context.location.region.value()
        try {
            val operation = compute.addresses().delete(projectId, region, resource.name).execute()
            return createOperationAwareCloudResource(resource, operation)
        } catch (e: GoogleJsonResponseException) {
            exceptionHandler(e, resource.name, resourceType())
            return null
        }

    }

    override fun resourceType(): ResourceType {
        return ResourceType.GCP_RESERVED_IP
    }

    override fun order(): Int {
        return ORDER
    }

    companion object {

        private val ORDER = 4
    }
}