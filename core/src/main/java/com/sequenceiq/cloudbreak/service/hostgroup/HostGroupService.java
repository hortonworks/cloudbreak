package com.sequenceiq.cloudbreak.service.hostgroup;

import javax.inject.Inject;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sequenceiq.cloudbreak.domain.HostGroup;
import com.sequenceiq.cloudbreak.repository.HostGroupRepository;

@Service
@Transactional
public class HostGroupService {

    @Inject
    private HostGroupRepository hostGroupRepository;

    public HostGroup getByClusterIdAndName(Long clusterId, String hostGroupName) {
        return hostGroupRepository.findHostGroupInClusterByName(clusterId, hostGroupName);
    }

    public HostGroup save(HostGroup hostGroup) {
        return hostGroupRepository.save(hostGroup);
    }

    public HostGroup getByClusterIdAndInstanceGroupName(Long clusterId, String instanceGroupName) {
        return hostGroupRepository.findHostGroupsByInstanceGroupName(clusterId, instanceGroupName);
    }
}
