package com.sequenceiq.cloudbreak.cloud.gcp.compute

import java.util.Arrays.asList

import java.io.IOException
import java.util.ArrayList
import java.util.Arrays

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.services.compute.Compute
import com.google.api.services.compute.model.AccessConfig
import com.google.api.services.compute.model.AttachedDisk
import com.google.api.services.compute.model.Instance
import com.google.api.services.compute.model.Metadata
import com.google.api.services.compute.model.NetworkInterface
import com.google.api.services.compute.model.Operation
import com.google.api.services.compute.model.Tags
import com.sequenceiq.cloudbreak.api.model.InstanceGroupType
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext
import com.sequenceiq.cloudbreak.cloud.context.CloudContext
import com.sequenceiq.cloudbreak.cloud.gcp.GcpResourceException
import com.sequenceiq.cloudbreak.cloud.gcp.context.GcpContext
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil
import com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance
import com.sequenceiq.cloudbreak.cloud.model.CloudResource
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus
import com.sequenceiq.cloudbreak.cloud.model.Group
import com.sequenceiq.cloudbreak.cloud.model.Image
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate
import com.sequenceiq.cloudbreak.cloud.model.Location
import com.sequenceiq.cloudbreak.cloud.model.Region
import com.sequenceiq.cloudbreak.common.type.ResourceType

@Component
class GcpInstanceResourceBuilder : AbstractGcpComputeBuilder() {

    override fun create(context: GcpContext, privateId: Long, auth: AuthenticatedContext, group: Group, image: Image): List<CloudResource> {
        val cloudContext = auth.cloudContext
        val resourceName = resourceNameService.resourceName(resourceType(), cloudContext.name, group.name, privateId)
        return Arrays.asList(createNamedResource(resourceType(), resourceName))
    }

    @Throws(Exception::class)
    override fun build(context: GcpContext, privateId: Long, auth: AuthenticatedContext, group: Group, image: Image,
                       buildableResource: List<CloudResource>): List<CloudResource> {
        val template = group.instances[0].template
        val projectId = context.projectId
        val location = context.location

        val compute = context.compute

        val computeResources = context.getComputeResources(privateId)
        val listOfDisks = ArrayList<AttachedDisk>()
        listOfDisks.addAll(getBootDiskList(computeResources, projectId, location.availabilityZone))
        listOfDisks.addAll(getAttachedDisks(computeResources, projectId, location.availabilityZone))

        val instance = Instance()
        instance.machineType = String.format("https://www.googleapis.com/compute/v1/projects/%s/zones/%s/machineTypes/%s",
                projectId, location.availabilityZone.value(), template.flavor)
        instance.name = buildableResource[0].name
        instance.canIpForward = java.lang.Boolean.TRUE
        instance.networkInterfaces = getNetworkInterface(context.networkResources, location.region, group, compute, projectId)
        instance.disks = listOfDisks

        val tags = Tags()
        val tagList = ArrayList<String>()
        tagList.add(group.name.toLowerCase().replace("[^A-Za-z0-9 ]".toRegex(), ""))
        tagList.add(GcpStackUtil.getClusterTag(auth.cloudContext))
        tags.items = tagList
        instance.tags = tags

        val metadata = Metadata()
        metadata.items = ArrayList<Metadata.Items>()

        val sshMetaData = Metadata.Items()
        sshMetaData.key = "sshKeys"
        sshMetaData.value = auth.cloudCredential.loginUserName + ":" + auth.cloudCredential.publicKey

        val startupScript = Metadata.Items()
        startupScript.key = "startup-script"
        startupScript.value = image.getUserData(group.type)

        metadata.items.add(sshMetaData)
        metadata.items.add(startupScript)
        instance.metadata = metadata

        val insert = compute.instances().insert(projectId, location.availabilityZone.value(), instance)
        insert.prettyPrint = java.lang.Boolean.TRUE
        try {
            val operation = insert.execute()
            if (operation.httpErrorStatusCode != null) {
                throw GcpResourceException(operation.httpErrorMessage, resourceType(), buildableResource[0].name)
            }
            return asList(createOperationAwareCloudResource(buildableResource[0], operation))
        } catch (e: GoogleJsonResponseException) {
            throw GcpResourceException(checkException(e), resourceType(), buildableResource[0].name)
        }

    }

    @Throws(Exception::class)
    override fun delete(context: GcpContext, auth: AuthenticatedContext, resource: CloudResource): CloudResource? {
        val resourceName = resource.name
        try {
            val operation = context.compute.instances().delete(context.projectId, context.location.availabilityZone.value(), resourceName).execute()
            return createOperationAwareCloudResource(resource, operation)
        } catch (e: GoogleJsonResponseException) {
            exceptionHandler(e, resourceName, resourceType())
        }

        return null
    }

    override fun checkInstances(context: GcpContext, auth: AuthenticatedContext, instances: List<CloudInstance>): List<CloudVmInstanceStatus>? {
        val cloudInstance = instances[0]
        try {
            LOGGER.info("Checking instance: {}", cloudInstance)
            val operation = check(context, cloudInstance)
            val finished = operation == null || GcpStackUtil.analyzeOperation(operation)
            val status = if (finished) if (context.isBuild) InstanceStatus.STARTED else InstanceStatus.STOPPED else InstanceStatus.IN_PROGRESS
            LOGGER.info("Instance: {} status: {}", instances, status)
            return asList(CloudVmInstanceStatus(cloudInstance, status))
        } catch (e: Exception) {
            LOGGER.info("Failed to check instance state of {}", cloudInstance)
            return asList(CloudVmInstanceStatus(cloudInstance, InstanceStatus.IN_PROGRESS))
        }

    }

    override fun stop(context: GcpContext, auth: AuthenticatedContext, instance: CloudInstance): CloudVmInstanceStatus? {
        return stopStart(context, auth, instance, true)
    }

    override fun start(context: GcpContext, auth: AuthenticatedContext, instance: CloudInstance): CloudVmInstanceStatus? {
        return stopStart(context, auth, instance, false)
    }

    override fun resourceType(): ResourceType {
        return ResourceType.GCP_INSTANCE
    }

    override fun order(): Int {
        return 2
    }

    private fun getBootDiskList(resources: List<CloudResource>, projectId: String, zone: AvailabilityZone): List<AttachedDisk> {
        val listOfDisks = ArrayList<AttachedDisk>()
        for (resource in filterResourcesByType(resources, ResourceType.GCP_DISK)) {
            listOfDisks.add(createDisk(resource, projectId, zone, true))
        }
        return listOfDisks
    }

    private fun getAttachedDisks(resources: List<CloudResource>, projectId: String, zone: AvailabilityZone): List<AttachedDisk> {
        val listOfDisks = ArrayList<AttachedDisk>()
        for (resource in filterResourcesByType(resources, ResourceType.GCP_ATTACHED_DISK)) {
            listOfDisks.add(createDisk(resource, projectId, zone, false))
        }
        return listOfDisks
    }

    private fun createDisk(resource: CloudResource, projectId: String, zone: AvailabilityZone, boot: Boolean): AttachedDisk {
        val attachedDisk = AttachedDisk()
        attachedDisk.boot = boot
        attachedDisk.autoDelete = true
        attachedDisk.type = GCP_DISK_TYPE
        attachedDisk.mode = GCP_DISK_MODE
        attachedDisk.deviceName = resource.name
        attachedDisk.source = String.format("https://www.googleapis.com/compute/v1/projects/%s/zones/%s/disks/%s",
                projectId, zone.value(), resource.name)
        return attachedDisk
    }

    @Throws(IOException::class)
    private fun getNetworkInterface(resources: List<CloudResource>,
                                    region: Region, group: Group, compute: Compute, projectId: String): List<NetworkInterface> {
        val networkInterface = NetworkInterface()
        val subnet = filterResourcesByType(resources, ResourceType.GCP_SUBNET)
        val networkName = if (subnet.isEmpty()) filterResourcesByType(resources, ResourceType.GCP_NETWORK)[0].name else subnet[0].name
        networkInterface.name = networkName
        val accessConfig = AccessConfig()
        accessConfig.name = networkName
        accessConfig.type = "ONE_TO_ONE_NAT"
        if (InstanceGroupType.GATEWAY == group.type) {
            val getReservedIp = compute.addresses().get(projectId, region.value(),
                    filterResourcesByType(resources, ResourceType.GCP_RESERVED_IP)[0].name)
            accessConfig.natIP = getReservedIp.execute().address
        }
        networkInterface.accessConfigs = asList(accessConfig)
        if (subnet.isEmpty()) {
            networkInterface.network = String.format("https://www.googleapis.com/compute/v1/projects/%s/global/networks/%s", projectId, networkName)
        } else {
            networkInterface.subnetwork = String.format("https://www.googleapis.com/compute/v1/projects/%s/regions/%s/subnetworks/%s", projectId, region.value(), networkName)
        }
        return asList(networkInterface)
    }

    private fun filterResourcesByType(resources: Collection<CloudResource>, resourceType: ResourceType): List<CloudResource> {
        val resourcesTemp = ArrayList<CloudResource>()
        for (resource in resources) {
            if (resourceType == resource.type) {
                resourcesTemp.add(resource)
            }
        }
        return resourcesTemp
    }

    private fun stopStart(context: GcpContext, auth: AuthenticatedContext, instance: CloudInstance, stopRequest: Boolean): CloudVmInstanceStatus? {
        val projectId = GcpStackUtil.getProjectId(auth.cloudCredential)
        val availabilityZone = context.location.availabilityZone.value()
        val compute = context.compute
        val instanceId = instance.instanceId
        try {
            val get = compute.instances().get(projectId, availabilityZone, instanceId)
            val state = if (stopRequest) "RUNNING" else "TERMINATED"
            if (state == get.execute().status) {
                val operation = if (stopRequest)
                    compute.instances().stop(projectId, availabilityZone, instanceId).setPrettyPrint(true).execute()
                else
                    compute.instances().start(projectId, availabilityZone, instanceId).setPrettyPrint(true).execute()
                val operationAwareCloudInstance = createOperationAwareCloudInstance(instance, operation)
                return checkInstances(context, auth, asList(operationAwareCloudInstance))!![0]
            } else {
                LOGGER.info("Instance {} is not in {} state - won't start it.", state, instanceId)
                return null
            }
        } catch (e: IOException) {
            throw GcpResourceException(String.format("An error occurred while stopping the vm '%s'", instanceId), e)
        }

    }

    companion object {

        private val LOGGER = LoggerFactory.getLogger(GcpInstanceResourceBuilder::class.java)
        private val GCP_DISK_TYPE = "PERSISTENT"
        private val GCP_DISK_MODE = "READ_WRITE"
    }

}
