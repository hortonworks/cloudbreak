package com.sequenceiq.cloudbreak.cloud.openstack.nativ.compute

import java.util.ArrayList
import java.util.Collections
import java.util.concurrent.Callable
import java.util.concurrent.Future

import javax.inject.Inject

import org.openstack4j.api.Builders
import org.openstack4j.api.OSClient
import org.openstack4j.api.exceptions.OS4JException
import org.openstack4j.model.compute.ActionResponse
import org.openstack4j.model.storage.block.Volume
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.task.AsyncTaskExecutor
import org.springframework.stereotype.Service

import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext
import com.sequenceiq.cloudbreak.cloud.context.CloudContext
import com.sequenceiq.cloudbreak.cloud.model.CloudResource
import com.sequenceiq.cloudbreak.cloud.model.Group
import com.sequenceiq.cloudbreak.cloud.model.Image
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate
import com.sequenceiq.cloudbreak.cloud.openstack.common.OpenStackConstants
import com.sequenceiq.cloudbreak.cloud.openstack.nativ.OpenStackResourceException
import com.sequenceiq.cloudbreak.cloud.openstack.nativ.context.OpenStackContext
import com.sequenceiq.cloudbreak.cloud.openstack.nativ.service.OpenStackResourceNameService
import com.sequenceiq.cloudbreak.cloud.openstack.view.CinderVolumeView
import com.sequenceiq.cloudbreak.cloud.openstack.view.NovaInstanceView
import com.sequenceiq.cloudbreak.common.type.ResourceType

@Service
class OpenStackAttachedDiskResourceBuilder : AbstractOpenStackComputeResourceBuilder() {

    @Inject
    private val resourceNameService: OpenStackResourceNameService? = null
    @Inject
    @Qualifier("intermediateBuilderExecutor")
    private val intermediateBuilderExecutor: AsyncTaskExecutor? = null

    override fun create(context: OpenStackContext, privateId: Long, auth: AuthenticatedContext, group: Group, image: Image): List<CloudResource> {
        val cloudResources = ArrayList<CloudResource>()
        val template = getInstanceTemplate(group, privateId)
        val instanceView = NovaInstanceView(template, group.type)
        val groupName = group.name
        val cloudContext = auth.cloudContext
        val stackName = cloudContext.name
        for (i in 0..instanceView.volumes.size - 1) {
            val resourceName = resourceNameService!!.resourceName(resourceType(), stackName, groupName, privateId, i)
            val resource = createNamedResource(resourceType(), resourceName)
            resource.putParameter(VOLUME_VIEW, instanceView.volumes[i])
            cloudResources.add(resource)
        }
        return cloudResources
    }

    @Throws(Exception::class)
    override fun build(context: OpenStackContext, privateId: Long, auth: AuthenticatedContext, group: Group, image: Image,
                       buildableResource: List<CloudResource>): List<CloudResource> {
        val resources = ArrayList<CloudResource>()
        val syncedResources = Collections.synchronizedList(resources)
        val futures = ArrayList<Future<Void>>()
        for (cloudResource in buildableResource) {
            val submit = intermediateBuilderExecutor!!.submit(Callable<java.lang.Void> {
                val volumeView = cloudResource.getParameter<CinderVolumeView>(VOLUME_VIEW, CinderVolumeView::class.java)
                var osVolume = Builders.volume().name(cloudResource.name).size(volumeView.size).build()
                try {
                    val osClient = createOSClient(auth)
                    osVolume = osClient.blockStorage().volumes().create(osVolume)
                    val newRes = createPersistedResource(cloudResource, osVolume.id)
                    newRes.putParameter(OpenStackConstants.VOLUME_MOUNT_POINT, volumeView.device)
                    syncedResources.add(newRes)
                } catch (ex: OS4JException) {
                    throw OpenStackResourceException("Volume creation failed", resourceType(), cloudResource.name, ex)
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
    override fun delete(context: OpenStackContext, auth: AuthenticatedContext, resource: CloudResource): CloudResource {
        try {
            val osClient = createOSClient(auth)
            val response = osClient.blockStorage().volumes().delete(resource.reference)
            return checkDeleteResponse(response, resourceType(), auth, resource, "Volume deletion failed")
        } catch (ex: OS4JException) {
            throw OpenStackResourceException("Volume deletion failed", resourceType(), resource.name, ex)
        }

    }

    override fun resourceType(): ResourceType {
        return ResourceType.OPENSTACK_ATTACHED_DISK
    }

    override fun checkStatus(context: OpenStackContext, auth: AuthenticatedContext, resource: CloudResource): Boolean {
        val cloudContext = auth.cloudContext
        val osClient = createOSClient(auth)
        val osVolume = osClient.blockStorage().volumes().get(resource.reference)
        if (osVolume != null && context.isBuild) {
            val volumeStatus = osVolume.status
            if (Volume.Status.ERROR == volumeStatus || Volume.Status.ERROR_DELETING == volumeStatus
                    || Volume.Status.ERROR_RESTORING == osVolume.status) {
                throw OpenStackResourceException("Volume in failed state", resource.type, resource.name, cloudContext.id,
                        volumeStatus.name)
            }
            return volumeStatus == Volume.Status.AVAILABLE
        } else if (osVolume == null && !context.isBuild) {
            return true
        }
        return false
    }

    companion object {
        private val VOLUME_VIEW = "volumeView"
    }
}
