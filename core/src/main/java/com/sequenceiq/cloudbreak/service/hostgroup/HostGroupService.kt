package com.sequenceiq.cloudbreak.service.hostgroup

import javax.inject.Inject
import javax.transaction.Transactional
import java.util.HashSet

import com.sequenceiq.cloudbreak.common.type.HostMetadataState
import com.sequenceiq.cloudbreak.domain.Cluster
import com.sequenceiq.cloudbreak.domain.HostGroup
import com.sequenceiq.cloudbreak.domain.HostMetadata
import com.sequenceiq.cloudbreak.repository.ConstraintRepository
import com.sequenceiq.cloudbreak.repository.HostGroupRepository
import com.sequenceiq.cloudbreak.repository.HostMetadataRepository
import org.springframework.stereotype.Service

@Service
@Transactional
class HostGroupService {

    @Inject
    private val hostGroupRepository: HostGroupRepository? = null

    @Inject
    private val hostMetadataRepository: HostMetadataRepository? = null

    @Inject
    private val constraintRepository: ConstraintRepository? = null

    fun getByCluster(clusterId: Long?): Set<HostGroup> {
        return hostGroupRepository!!.findHostGroupsInCluster(clusterId)
    }

    fun getByClusterIdAndName(clusterId: Long?, hostGroupName: String): HostGroup {
        return hostGroupRepository!!.findHostGroupInClusterByName(clusterId, hostGroupName)
    }

    fun save(hostGroup: HostGroup): HostGroup {
        return hostGroupRepository!!.save(hostGroup)
    }

    fun findEmptyHostMetadataInHostGroup(hostGroupId: Long?): Set<HostMetadata> {
        return hostMetadataRepository!!.findEmptyContainerHostsInHostGroup(hostGroupId)
    }

    fun getByClusterIdAndInstanceGroupName(clusterId: Long?, instanceGroupName: String): HostGroup {
        return hostGroupRepository!!.findHostGroupsByInstanceGroupName(clusterId, instanceGroupName)
    }

    fun updateHostMetaDataStatus(id: Long?, status: HostMetadataState): HostMetadata {
        val metaData = hostMetadataRepository!!.findOne(id)
        metaData.hostMetadataState = status
        return hostMetadataRepository.save(metaData)
    }

    fun saveOrUpdateWithMetadata(hostGroups: Collection<HostGroup>, cluster: Cluster): Set<HostGroup> {
        val result = HashSet<HostGroup>(hostGroups.size)
        for (hg in hostGroups) {
            hg.cluster = cluster
            hg.constraint = constraintRepository!!.save(hg.constraint)
            result.add(hostGroupRepository!!.save(hg))
        }
        return result
    }

}
