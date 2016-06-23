package com.sequenceiq.cloudbreak.cloud.openstack.nativ.compute

import java.util.Collections

import org.apache.commons.codec.binary.Base64
import org.openstack4j.api.Builders
import org.openstack4j.api.OSClient
import org.openstack4j.api.exceptions.OS4JException
import org.openstack4j.model.compute.Action
import org.openstack4j.model.compute.ActionResponse
import org.openstack4j.model.compute.BDMDestType
import org.openstack4j.model.compute.BDMSourceType
import org.openstack4j.model.compute.BlockDeviceMappingCreate
import org.openstack4j.model.compute.Flavor
import org.openstack4j.model.compute.Server
import org.openstack4j.model.compute.ServerCreate
import org.openstack4j.model.compute.builder.BlockDeviceMappingBuilder
import org.openstack4j.model.compute.builder.ServerCreateBuilder
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

import com.google.common.collect.Lists
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext
import com.sequenceiq.cloudbreak.cloud.context.CloudContext
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance
import com.sequenceiq.cloudbreak.cloud.model.CloudResource
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus
import com.sequenceiq.cloudbreak.cloud.model.Group
import com.sequenceiq.cloudbreak.cloud.model.Image
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate
import com.sequenceiq.cloudbreak.cloud.openstack.common.OpenStackConstants
import com.sequenceiq.cloudbreak.cloud.openstack.nativ.OpenStackResourceException
import com.sequenceiq.cloudbreak.cloud.openstack.nativ.context.OpenStackContext
import com.sequenceiq.cloudbreak.cloud.openstack.status.NovaInstanceStatus
import com.sequenceiq.cloudbreak.cloud.openstack.view.KeystoneCredentialView
import com.sequenceiq.cloudbreak.cloud.openstack.view.NovaInstanceView
import com.sequenceiq.cloudbreak.common.type.ResourceType

@Service
class OpenStackInstanceBuilder : AbstractOpenStackComputeResourceBuilder() {


    @Throws(Exception::class)
    override fun build(context: OpenStackContext, privateId: Long, auth: AuthenticatedContext, group: Group, image: Image,
                       buildableResource: List<CloudResource>): List<CloudResource> {
        val resource = buildableResource[0]
        try {
            val osClient = createOSClient(auth)
            val template = getInstanceTemplate(group, privateId)
            val port = getPort(context.getComputeResources(privateId))
            val osCredential = KeystoneCredentialView(auth)
            val novaInstanceView = NovaInstanceView(template, group.type)
            val imageId = osClient.images().list(Collections.singletonMap("name", image.imageName))[0].id
            var serverCreateBuilder = Builders.server().name(resource.name).image(imageId).flavor(getFlavorId(osClient, novaInstanceView.flavor)).keypairName(osCredential.keyPairName).addMetadata(novaInstanceView.metadataMap).addNetworkPort(port.getStringParameter(OpenStackConstants.PORT_ID)).userData(String(Base64.encodeBase64(image.getUserData(group.type).toByteArray())))
            val blockDeviceMappingBuilder = Builders.blockDeviceMapping().uuid(imageId).sourceType(BDMSourceType.IMAGE).deviceName("/dev/vda").bootIndex(0).deleteOnTermination(true).destinationType(BDMDestType.LOCAL)
            serverCreateBuilder = serverCreateBuilder.blockDevice(blockDeviceMappingBuilder.build())
            for (computeResource in context.getComputeResources(privateId)) {
                if (computeResource.type === ResourceType.OPENSTACK_ATTACHED_DISK) {
                    val blockDeviceMappingCreate = Builders.blockDeviceMapping().uuid(computeResource.reference).deviceName(computeResource.getStringParameter(OpenStackConstants.VOLUME_MOUNT_POINT)).sourceType(BDMSourceType.VOLUME).destinationType(BDMDestType.VOLUME).build()
                    serverCreateBuilder.blockDevice(blockDeviceMappingCreate)
                }
            }
            val serverCreate = serverCreateBuilder.build()
            val server = osClient.compute().servers().boot(serverCreate)
            return listOf<CloudResource>(createPersistedResource(resource, server.id,
                    Collections.singletonMap<String, Any>(OpenStackConstants.SERVER, server)))
        } catch (ex: OS4JException) {
            LOGGER.error("Failed to create OpenStack instance with privateId: {}", privateId, ex)
            throw OpenStackResourceException("Instance creation failed", resourceType(), resource.name, ex)
        }

    }

    override fun checkInstances(context: OpenStackContext, auth: AuthenticatedContext, instances: List<CloudInstance>): List<CloudVmInstanceStatus>? {
        val statuses = Lists.newArrayList<CloudVmInstanceStatus>()
        val osClient = createOSClient(auth)
        for (instance in instances) {
            val server = osClient.compute().servers().get(instance.getStringParameter(OpenStackConstants.INSTANCE_ID))
            if (server == null) {
                statuses.add(CloudVmInstanceStatus(instance, InstanceStatus.TERMINATED))
            } else {
                statuses.add(CloudVmInstanceStatus(instance, NovaInstanceStatus.get(server)))
            }
        }
        return statuses
    }

    @Throws(Exception::class)
    override fun delete(context: OpenStackContext, auth: AuthenticatedContext, resource: CloudResource): CloudResource {
        try {
            val osClient = createOSClient(auth)
            val response = osClient.compute().servers().delete(resource.reference)
            return checkDeleteResponse(response, resourceType(), auth, resource, "Instance deletion failed")
        } catch (ex: OS4JException) {
            throw OpenStackResourceException("Instance deletion failed", resourceType(), resource.name, ex)
        }

    }

    override fun stop(context: OpenStackContext, auth: AuthenticatedContext, instance: CloudInstance): CloudVmInstanceStatus? {
        return executeAction(auth, instance, Action.STOP)
    }

    override fun start(context: OpenStackContext, auth: AuthenticatedContext, instance: CloudInstance): CloudVmInstanceStatus? {
        return executeAction(auth, instance, Action.START)
    }

    private fun executeAction(auth: AuthenticatedContext, instance: CloudInstance, action: Action): CloudVmInstanceStatus {
        val osClient = createOSClient(auth)
        val actionResponse = osClient.compute().servers().action(instance.instanceId, action)
        if (actionResponse.isSuccess) {
            return CloudVmInstanceStatus(instance, InstanceStatus.IN_PROGRESS)
        }
        return CloudVmInstanceStatus(instance, InstanceStatus.FAILED, actionResponse.fault)
    }


    override fun resourceType(): ResourceType {
        return ResourceType.OPENSTACK_INSTANCE
    }

    override fun checkStatus(context: OpenStackContext, auth: AuthenticatedContext, resource: CloudResource): Boolean {
        val status = getStatus(auth, resource.reference)
        if (status != null && context.isBuild) {
            if (Server.Status.ERROR == status) {
                val cloudContext = auth.cloudContext
                throw OpenStackResourceException("Instance in failed state", resource.type, resource.name, cloudContext.id,
                        status.name)
            }
            return status == Server.Status.ACTIVE
        } else if (status == null && !context.isBuild) {
            return true
        }
        return false
    }

    private fun getStatus(auth: AuthenticatedContext, serverId: String): Server.Status? {
        val osClient = createOSClient(auth)
        val server = osClient.compute().servers().get(serverId)
        var status: Server.Status? = null
        if (server != null) {
            status = server.status
        }
        return status
    }

    private fun getPort(computeResources: List<CloudResource>): CloudResource {
        var instance: CloudResource? = null
        for (computeResource in computeResources) {
            if (computeResource.type === ResourceType.OPENSTACK_PORT) {
                instance = computeResource
            }
        }
        return instance
    }

    private fun getFlavorId(osClient: OSClient, flavorName: String): String? {
        val flavors = osClient.compute().flavors().list()
        for (flavor in flavors) {
            if (flavor.name == flavorName) {
                return flavor.id
            }
        }
        return null
    }

    companion object {

        private val LOGGER = LoggerFactory.getLogger(OpenStackInstanceBuilder::class.java)
    }
}
