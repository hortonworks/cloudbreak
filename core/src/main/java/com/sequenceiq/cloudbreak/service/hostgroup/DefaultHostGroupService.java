package com.sequenceiq.cloudbreak.service.hostgroup;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.domain.HostGroup;
import com.sequenceiq.cloudbreak.repository.HostGroupRepository;

@Service
public class DefaultHostGroupService implements HostGroupService {

    @Inject private HostGroupRepository hostGroupRepository;

    @Override
    public HostGroup getByClusterIdAndName(Long clusterId, String hostGroupName) {
        return hostGroupRepository.findHostGroupInClusterByName(clusterId, hostGroupName);
    }

    @Override
    public HostGroup save(HostGroup hostGroup) {
        return hostGroupRepository.save(hostGroup);
    }

    @Override
    public HostGroup getByClusterIdAndInstanceGroupName(Long clusterId, String instanceGroupName) {
        return hostGroupRepository.findHostGroupsByInstanceGroupName(clusterId, instanceGroupName);
    }
}
