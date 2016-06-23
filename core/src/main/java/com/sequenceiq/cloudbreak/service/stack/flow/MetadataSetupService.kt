package com.sequenceiq.cloudbreak.service.stack.flow

import java.util.Calendar
import java.util.HashSet
import java.util.Objects

import javax.inject.Inject

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

import com.sequenceiq.cloudbreak.api.model.InstanceGroupType
import com.sequenceiq.cloudbreak.api.model.InstanceStatus
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance
import com.sequenceiq.cloudbreak.cloud.model.CloudInstanceMetaData
import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus
import com.sequenceiq.cloudbreak.domain.InstanceGroup
import com.sequenceiq.cloudbreak.domain.InstanceMetaData
import com.sequenceiq.cloudbreak.domain.Stack
import com.sequenceiq.cloudbreak.repository.InstanceGroupRepository
import com.sequenceiq.cloudbreak.repository.InstanceMetaDataRepository
import com.sequenceiq.cloudbreak.service.cluster.ClusterService
import com.sequenceiq.cloudbreak.service.stack.connector.adapter.ServiceProviderMetadataAdapter

@Service
class MetadataSetupService {
    @Inject
    private val metadata: ServiceProviderMetadataAdapter? = null
    @Inject
    private val instanceGroupRepository: InstanceGroupRepository? = null
    @Inject
    private val instanceMetaDataRepository: InstanceMetaDataRepository? = null
    @Inject
    private val clusterService: ClusterService? = null

    fun collectMetadata(stack: Stack) {
        saveInstanceMetaData(stack, collectCoreMetadata(stack), null)
    }

    fun saveInstanceMetaData(stack: Stack, cloudVmMetaDataStatusList: List<CloudVmMetaDataStatus>, status: InstanceStatus?): Set<InstanceMetaData> {
        var ambariServerFound: Boolean? = false
        val updatedInstanceMetadata = HashSet<InstanceMetaData>()
        val allInstanceMetadata = instanceMetaDataRepository!!.findNotTerminatedForStack(stack.id)
        for (cloudVmMetaDataStatus in cloudVmMetaDataStatusList) {
            val cloudInstance = cloudVmMetaDataStatus.cloudVmInstanceStatus.cloudInstance
            val md = cloudVmMetaDataStatus.metaData
            val timeInMillis = Calendar.getInstance().timeInMillis
            val privateId = cloudInstance.template!!.privateId
            val instanceId = cloudInstance.instanceId
            val instanceMetaDataEntry = createInstanceMetadataIfAbsent(allInstanceMetadata, privateId, instanceId)
            // CB 1.0.x clusters do not have private id thus we cannot correlate them with instance groups thus keep the original one
            val group = if (instanceMetaDataEntry.instanceGroup == null)
                cloudInstance.template!!.groupName
            else
                instanceMetaDataEntry.instanceGroup.groupName
            val instanceGroup = instanceGroupRepository!!.findOneByGroupNameInStack(stack.id, group)
            instanceMetaDataEntry.privateIp = md.privateIp
            instanceMetaDataEntry.publicIp = md.publicIp
            instanceMetaDataEntry.sshPort = md.sshPort
            instanceMetaDataEntry.hypervisor = md.hypervisor
            instanceMetaDataEntry.instanceGroup = instanceGroup
            instanceMetaDataEntry.instanceId = instanceId
            instanceMetaDataEntry.privateId = privateId
            instanceMetaDataEntry.startDate = timeInMillis
            if ((!ambariServerFound)!! && InstanceGroupType.GATEWAY == instanceGroup.instanceGroupType) {
                instanceMetaDataEntry.ambariServer = java.lang.Boolean.TRUE
                ambariServerFound = true
            } else {
                instanceMetaDataEntry.ambariServer = java.lang.Boolean.FALSE
            }
            if (status != null) {
                instanceMetaDataEntry.instanceStatus = status
            }
            instanceMetaDataRepository.save(instanceMetaDataEntry)
            updatedInstanceMetadata.add(instanceMetaDataEntry)
        }
        return updatedInstanceMetadata
    }

    private fun collectCoreMetadata(stack: Stack): List<CloudVmMetaDataStatus> {
        val coreInstanceMetaData = metadata!!.collectMetadata(stack)
        if (coreInstanceMetaData.size != stack.fullNodeCount) {
            throw WrongMetadataException(String.format(
                    "Size of the collected metadata set does not equal the node count of the stack. [metadata size=%s] [nodecount=%s]",
                    coreInstanceMetaData.size, stack.fullNodeCount))
        }
        return coreInstanceMetaData
    }

    private fun createInstanceMetadataIfAbsent(allInstanceMetadata: Set<InstanceMetaData>, privateId: Long?, instanceId: String): InstanceMetaData {
        if (privateId != null) {
            for (instanceMetaData in allInstanceMetadata) {
                if (instanceMetaData.privateId == privateId) {
                    return instanceMetaData
                }
            }
        } else {
            for (instanceMetaData in allInstanceMetadata) {
                if (instanceMetaData.instanceId == instanceId) {
                    return instanceMetaData
                }
            }
        }
        return InstanceMetaData()
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(MetadataSetupService::class.java)
    }

}
