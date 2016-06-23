package com.sequenceiq.cloudbreak.cloud.gcp.compute

import java.util.Arrays

import org.springframework.stereotype.Component

import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.services.compute.Compute
import com.google.api.services.compute.model.Disk
import com.google.api.services.compute.model.Operation
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext
import com.sequenceiq.cloudbreak.cloud.context.CloudContext
import com.sequenceiq.cloudbreak.cloud.gcp.GcpPlatformParameters
import com.sequenceiq.cloudbreak.cloud.gcp.GcpResourceException
import com.sequenceiq.cloudbreak.cloud.gcp.context.GcpContext
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil
import com.sequenceiq.cloudbreak.cloud.model.CloudResource
import com.sequenceiq.cloudbreak.cloud.model.Group
import com.sequenceiq.cloudbreak.cloud.model.Image
import com.sequenceiq.cloudbreak.cloud.model.Location
import com.sequenceiq.cloudbreak.common.type.ResourceType

@Component
class GcpDiskResourceBuilder : AbstractGcpComputeBuilder() {

    override fun create(context: GcpContext, privateId: Long, auth: AuthenticatedContext, group: Group, image: Image): List<CloudResource> {
        val cloudContext = auth.cloudContext
        val resourceName = resourceNameService.resourceName(resourceType(), cloudContext.name, group.name, privateId)
        return Arrays.asList(createNamedResource(resourceType(), resourceName))
    }

    @Throws(Exception::class)
    override fun build(context: GcpContext, privateId: Long, auth: AuthenticatedContext, group: Group, image: Image,
                       buildableResources: List<CloudResource>): List<CloudResource> {
        val projectId = context.projectId
        val location = context.location

        val disk = Disk()
        disk.sizeGb = DEFAULT_ROOT_DISK_SIZE
        disk.name = buildableResources[0].name
        disk.kind = GcpPlatformParameters.GcpDiskType.HDD.getUrl(projectId, location.availabilityZone)

        val insDisk = context.compute.disks().insert(projectId, location.availabilityZone.value(), disk)
        insDisk.sourceImage = GcpStackUtil.getAmbariImage(projectId, image.imageName)
        try {
            val operation = insDisk.execute()
            if (operation.httpErrorStatusCode != null) {
                throw GcpResourceException(operation.httpErrorMessage, resourceType(), buildableResources[0].name)
            }
            return Arrays.asList(createOperationAwareCloudResource(buildableResources[0], operation))
        } catch (e: GoogleJsonResponseException) {
            throw GcpResourceException(checkException(e), resourceType(), buildableResources[0].name)
        }

    }

    @Throws(Exception::class)
    override fun delete(context: GcpContext, auth: AuthenticatedContext, resource: CloudResource): CloudResource? {
        val resourceName = resource.name
        try {
            val operation = context.compute.disks().delete(context.projectId, context.location.availabilityZone.value(), resourceName).execute()
            return createOperationAwareCloudResource(resource, operation)
        } catch (e: GoogleJsonResponseException) {
            exceptionHandler(e, resourceName, resourceType())
        }

        return null
    }

    override fun resourceType(): ResourceType {
        return ResourceType.GCP_DISK
    }

    override fun order(): Int {
        return 0
    }

    companion object {

        private val DEFAULT_ROOT_DISK_SIZE = 50L
    }
}
