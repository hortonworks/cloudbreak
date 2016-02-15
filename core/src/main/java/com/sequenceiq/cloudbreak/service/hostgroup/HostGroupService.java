package com.sequenceiq.cloudbreak.service.hostgroup;

import javax.inject.Inject;
import javax.transaction.Transactional;

import java.util.Set;

import com.sequenceiq.cloudbreak.common.type.HostMetadataState;
import com.sequenceiq.cloudbreak.domain.HostGroup;
import com.sequenceiq.cloudbreak.domain.HostMetadata;
import com.sequenceiq.cloudbreak.repository.HostGroupRepository;
import com.sequenceiq.cloudbreak.repository.HostMetadataRepository;
import org.springframework.stereotype.Service;

@Service
@Transactional
public class HostGroupService {

    @Inject
    private HostGroupRepository hostGroupRepository;

    @Inject
    private HostMetadataRepository hostMetadataRepository;

    public Set<HostGroup> getByCluster(Long clusterId) {
        return hostGroupRepository.findHostGroupsInCluster(clusterId);
    }

    public HostGroup getByClusterIdAndName(Long clusterId, String hostGroupName) {
        return hostGroupRepository.findHostGroupInClusterByName(clusterId, hostGroupName);
    }

    @Transactional(javax.transaction.Transactional.TxType.NEVER)
    public HostGroup save(HostGroup hostGroup) {
        return hostGroupRepository.save(hostGroup);
    }

    public Set<HostMetadata> findEmptyHostMetadataInHostGroup(Long hostGroupId) {
        return hostMetadataRepository.findEmptyContainerHostsInHostGroup(hostGroupId);
    }

    public HostGroup getByClusterIdAndInstanceGroupName(Long clusterId, String instanceGroupName) {
        return hostGroupRepository.findHostGroupsByInstanceGroupName(clusterId, instanceGroupName);
    }

    public HostMetadata updateHostMetaDataStatus(Long id, HostMetadataState status) {
        HostMetadata metaData = hostMetadataRepository.findOne(id);
        metaData.setHostMetadataState(status);
        return hostMetadataRepository.save(metaData);
    }

}
