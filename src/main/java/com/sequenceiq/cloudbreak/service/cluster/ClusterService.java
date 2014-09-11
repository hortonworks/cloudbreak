package com.sequenceiq.cloudbreak.service.cluster;

import java.util.Set;

import com.sequenceiq.cloudbreak.controller.json.HostGroupAdjustmentJson;
import com.sequenceiq.cloudbreak.domain.CbUser;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.StatusRequest;

public interface ClusterService {
    void create(CbUser user, Long stackId, Cluster clusterRequest);

    Cluster retrieveCluster(Long stackId);

    String getClusterJson(String ambariIp, Long stackId);

    void updateHosts(Long stackId, Set<HostGroupAdjustmentJson> hostGroupAdjustments);

    void updateStatus(User user, Long stackId, StatusRequest statusRequest);
}
