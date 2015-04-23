package com.sequenceiq.cloudbreak.service.hostgroup;

import com.sequenceiq.cloudbreak.domain.HostGroup;

public interface HostGroupService {

    HostGroup getByClusterIdAndInstanceGroupName(Long clusterId, String instanceGroupName);

    HostGroup getByClusterIdAndName(Long clusterId, String hostGroupName);

    HostGroup save(HostGroup hostGroup);

}
