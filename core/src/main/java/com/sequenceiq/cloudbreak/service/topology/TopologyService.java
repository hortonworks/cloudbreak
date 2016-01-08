package com.sequenceiq.cloudbreak.service.topology;

import com.sequenceiq.cloudbreak.domain.CbUser;
import com.sequenceiq.cloudbreak.domain.Topology;

public interface TopologyService {
    Topology create(CbUser user, Topology templateRequest);
    void delete(Long topologyId, CbUser user);
}
