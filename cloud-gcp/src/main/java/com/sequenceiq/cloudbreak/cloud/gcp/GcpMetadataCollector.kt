package com.sequenceiq.cloudbreak.cloud.gcp

import java.io.IOException
import java.util.ArrayList
import java.util.HashMap

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

import com.google.api.services.compute.Compute
import com.google.api.services.compute.model.Instance
import com.sequenceiq.cloudbreak.cloud.MetadataCollector
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext
import com.sequenceiq.cloudbreak.cloud.context.CloudContext
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance
import com.sequenceiq.cloudbreak.cloud.model.CloudInstanceMetaData
import com.sequenceiq.cloudbreak.cloud.model.CloudResource
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus
import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus
import com.sequenceiq.cloudbreak.common.type.ResourceType

@Service
class GcpMetadataCollector : MetadataCollector {

    override fun collect(authenticatedContext: AuthenticatedContext, resources: List<CloudResource>, vms: List<CloudInstance>): List<CloudVmMetaDataStatus> {

        val instanceMetaData = ArrayList<CloudVmMetaDataStatus>()

        val instanceNameMap = groupByInstanceName(resources)
        val privateIdMap = groupByPrivateId(resources)

        for (cloudInstance in vms) {
            val instanceId = cloudInstance.instanceId
            val cloudResource: CloudResource
            if (instanceId != null) {
                cloudResource = instanceNameMap[instanceId]
            } else {
                cloudResource = privateIdMap.get(cloudInstance.template!!.privateId)
            }
            val cloudVmMetaDataStatus = getCloudVmMetaDataStatus(authenticatedContext, cloudResource, cloudInstance)
            instanceMetaData.add(cloudVmMetaDataStatus)
        }

        return instanceMetaData
    }

    private fun getCloudVmMetaDataStatus(authenticatedContext: AuthenticatedContext, cloudResource: CloudResource?,
                                         matchedInstance: CloudInstance): CloudVmMetaDataStatus {
        val cloudVmMetaDataStatus: CloudVmMetaDataStatus
        if (cloudResource != null) {
            val cloudInstance = CloudInstance(cloudResource.name, matchedInstance.template)
            try {
                val credential = authenticatedContext.cloudCredential
                val cloudContext = authenticatedContext.cloudContext
                val compute = GcpStackUtil.buildCompute(credential)
                val executeInstance = getInstance(cloudContext, credential, compute, cloudResource.name)


                val metaData = CloudInstanceMetaData(
                        executeInstance.networkInterfaces[0].networkIP,
                        executeInstance.networkInterfaces[0].accessConfigs[0].natIP)

                val status = CloudVmInstanceStatus(cloudInstance, InstanceStatus.CREATED)
                cloudVmMetaDataStatus = CloudVmMetaDataStatus(status, metaData)

            } catch (e: IOException) {
                LOGGER.warn(String.format("Instance %s is not reachable", cloudResource.name), e)
                val status = CloudVmInstanceStatus(cloudInstance, InstanceStatus.UNKNOWN)
                cloudVmMetaDataStatus = CloudVmMetaDataStatus(status, CloudInstanceMetaData.EMPTY_METADATA)
            }

        } else {
            val status = CloudVmInstanceStatus(matchedInstance, InstanceStatus.TERMINATED)
            cloudVmMetaDataStatus = CloudVmMetaDataStatus(status, CloudInstanceMetaData.EMPTY_METADATA)
        }
        return cloudVmMetaDataStatus

    }

    private fun groupByInstanceName(resources: List<CloudResource>): Map<String, CloudResource> {
        val instanceNameMap = HashMap<String, CloudResource>()
        for (resource in resources) {
            if (ResourceType.GCP_INSTANCE === resource.type) {
                val resourceName = resource.name
                instanceNameMap.put(resourceName, resource)
            }
        }
        return instanceNameMap
    }

    private fun groupByPrivateId(resources: List<CloudResource>): Map<Long, CloudResource> {
        val privateIdMap = HashMap<Long, CloudResource>()
        for (resource in resources) {
            if (ResourceType.GCP_INSTANCE === resource.type) {
                val resourceName = resource.name
                val privateId = GcpStackUtil.getPrivateId(resourceName)
                if (privateId != null) {
                    privateIdMap.put(privateId, resource)
                }

            }
        }
        return privateIdMap
    }

    @Throws(IOException::class)
    private fun getInstance(context: CloudContext, credential: CloudCredential, compute: Compute, instanceName: String): Instance {
        return compute.instances().get(GcpStackUtil.getProjectId(credential),
                context.location!!.availabilityZone.value(), instanceName).execute()
    }

    companion object {

        private val LOGGER = LoggerFactory.getLogger(GcpMetadataCollector::class.java)
    }

}
