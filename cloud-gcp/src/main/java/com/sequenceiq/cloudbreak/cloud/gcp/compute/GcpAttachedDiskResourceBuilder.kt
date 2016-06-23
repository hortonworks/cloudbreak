package com.sequenceiq.cloudbreak.cloud.gcp.compute

import java.util.ArrayList
import java.util.Collections
import java.util.concurrent.Callable
import java.util.concurrent.Future

import javax.inject.Inject

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.task.AsyncTaskExecutor
import org.springframework.stereotype.Component

import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.services.compute.Compute
import com.google.api.services.compute.model.Disk
import com.google.api.services.compute.model.Operation
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext
import com.sequenceiq.cloudbreak.cloud.context.CloudContext
import com.sequenceiq.cloudbreak.cloud.gcp.GcpPlatformParameters.GcpDiskType
import com.sequenceiq.cloudbreak.cloud.gcp.GcpResourceException
import com.sequenceiq.cloudbreak.cloud.gcp.context.GcpContext
import com.sequenceiq.cloudbreak.cloud.gcp.service.GcpResourceNameService
import com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance
import com.sequenceiq.cloudbreak.cloud.model.CloudResource
import com.sequenceiq.cloudbreak.cloud.model.Group
import com.sequenceiq.cloudbreak.cloud.model.Image
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate
import com.sequenceiq.cloudbreak.cloud.model.Location
import com.sequenceiq.cloudbreak.cloud.model.Volume
import com.sequenceiq.cloudbreak.common.type.ResourceType

@Component
class GcpAttachedDiskResourceBuilder : AbstractGcpComputeBuilder() {

    @Inject
    @Qualifier("intermediateBuilderExecutor")
    private val intermediateBuilderExecutor: AsyncTaskExecutor? = null

    override fun create(context: GcpContext, privateId: Long, auth: AuthenticatedContext, group: Group, image: Image): List<CloudResource> {
        val cloudResources = ArrayList<CloudResource>()
        val instance = group.instances[0]
        val template = instance.template
        val resourceNameService = resourceNameService
        val groupName = group.name
        val cloudContext = auth.cloudContext
        val stackName = cloudContext.name
        for (i in 0..template.volumes.size - 1) {
            val resourceName = resourceNameService.resourceName(resourceType(), stackName, groupName, privateId, i)
            cloudResources.add(createNamedResource(resourceType(), resourceName))
        }
        return cloudResources
    }

    @Throws(Exception::class)
    override fun build(context: GcpContext, privateId: Long, auth: AuthenticatedContext, group: Group, image: Image,
                       buildableResource: List<CloudResource>): List<CloudResource> {
        val instance = group.instances[0]
        val template = instance.template
        val volume = template.volumes[0]

        val resources = ArrayList<CloudResource>()
        val syncedResources = Collections.synchronizedList(resources)
        val projectId = context.projectId
        val location = context.location
        val compute = context.compute
        val futures = ArrayList<Future<Void>>()
        for (cloudResource in buildableResource) {
            val disk = createDisk(volume, projectId, location.availabilityZone, cloudResource.name)
            val submit = intermediateBuilderExecutor!!.submit(Callable<java.lang.Void> {
                val insDisk = compute.disks().insert(projectId, location.availabilityZone.value(), disk)
                try {
                    val operation = insDisk.execute()
                    syncedResources.add(createOperationAwareCloudResource(cloudResource, operation))
                    if (operation.httpErrorStatusCode != null) {
                        throw GcpResourceException(operation.httpErrorMessage, resourceType(), cloudResource.name)
                    }
                } catch (e: GoogleJsonResponseException) {
                    throw GcpResourceException(checkException(e), resourceType(), cloudResource.name)
                }

                null
            })
            futures.add(submit)
        }
        for (future in futures) {
            future.get()
        }
        return resources
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
        return ResourceType.GCP_ATTACHED_DISK
    }

    override fun order(): Int {
        return 1
    }

    private fun createDisk(volume: Volume, projectId: String, availabilityZone: AvailabilityZone, resourceName: String): Disk {
        val disk = Disk()
        disk.sizeGb = volume.size.toLong()
        disk.name = resourceName
        disk.type = GcpDiskType.getUrl(projectId, availabilityZone, volume.type)
        return disk
    }
}
