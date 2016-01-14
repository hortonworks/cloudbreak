package com.sequenceiq.cloudbreak.service.hostgroup;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.springframework.stereotype.Service;

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

    @Transactional(javax.transaction.Transactional.TxType.NEVER)
    public HostGroup save(HostGroup hostGroup) {
        return hostGroupRepository.save(hostGroup);
    }

}
