package com.sequenceiq.cloudbreak.service.stack

import javax.inject.Inject

import org.springframework.stereotype.Service

import com.sequenceiq.cloudbreak.cloud.model.CloudInstance
import com.sequenceiq.cloudbreak.cloud.model.Group
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate
import com.sequenceiq.cloudbreak.api.model.InstanceGroupType
import com.sequenceiq.cloudbreak.domain.InstanceGroup
import com.sequenceiq.cloudbreak.domain.InstanceMetaData
import com.sequenceiq.cloudbreak.domain.Stack
import com.sequenceiq.cloudbreak.repository.InstanceMetaDataRepository

@Service
class InstanceMetadataService {

    @Inject
    private val instanceMetaDataRepository: InstanceMetaDataRepository? = null

    fun updateInstanceStatus(instanceGroup: Set<InstanceGroup>,
                             newStatusByGroupType: Map<InstanceGroupType, com.sequenceiq.cloudbreak.api.model.InstanceStatus>) {
        for (group in instanceGroup) {
            val newStatus = newStatusByGroupType[group.instanceGroupType]
            if (newStatus != null) {
                for (instanceMetaData in group.instanceMetaData) {
                    instanceMetaData.instanceStatus = newStatus
                    instanceMetaDataRepository!!.save(instanceMetaData)
                }
            }
        }
    }

    fun updateInstanceStatus(instanceGroup: Set<InstanceGroup>, newStatus: com.sequenceiq.cloudbreak.api.model.InstanceStatus,
                             candidateAddresses: Set<String>) {
        for (group in instanceGroup) {
            for (instanceMetaData in group.instanceMetaData) {
                if (candidateAddresses.contains(instanceMetaData.discoveryFQDN)) {
                    instanceMetaData.instanceStatus = newStatus
                    instanceMetaDataRepository!!.save(instanceMetaData)
                }
            }
        }
    }

    fun saveInstanceRequests(stack: Stack, groups: List<Group>) {
        val instanceGroups = stack.instanceGroups
        for (group in groups) {
            val instanceGroup = getInstanceGroup(instanceGroups, group.name)
            for (cloudInstance in group.instances) {
                val instanceTemplate = cloudInstance.template
                if (InstanceStatus.CREATE_REQUESTED === instanceTemplate.status) {
                    val instanceMetaData = InstanceMetaData()
                    instanceMetaData.privateId = instanceTemplate.privateId
                    instanceMetaData.instanceStatus = com.sequenceiq.cloudbreak.api.model.InstanceStatus.REQUESTED
                    instanceMetaData.instanceGroup = instanceGroup
                    instanceMetaDataRepository!!.save(instanceMetaData)
                }
            }
        }
    }

    fun deleteInstanceRequest(stackId: Long?, privateId: Long?) {
        val instanceMetaData = instanceMetaDataRepository!!.findAllInStack(stackId)
        for (metaData in instanceMetaData) {
            if (metaData.privateId == privateId) {
                instanceMetaDataRepository.delete(metaData)
                break
            }
        }
    }

    private fun getInstanceGroup(instanceGroups: Set<InstanceGroup>, groupName: String): InstanceGroup? {
        for (instanceGroup in instanceGroups) {
            if (groupName.equals(instanceGroup.groupName, ignoreCase = true)) {
                return instanceGroup
            }
        }
        return null
    }
}
