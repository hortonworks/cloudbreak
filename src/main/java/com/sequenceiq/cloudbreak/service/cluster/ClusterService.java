package com.sequenceiq.cloudbreak.service.cluster;

import java.util.Set;

import com.sequenceiq.cloudbreak.controller.json.HostGroupAdjustmentJson;
import com.sequenceiq.cloudbreak.domain.CbUser;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.StatusRequest;

public interface ClusterService {
    void create(CbUser user, Long stackId, Cluster clusterRequest);

    void create(CbUser user, String name, Cluster clusterRequest);

    Cluster retrieveCluster(Long stackId);

    Cluster retrieveCluster(String stackName);

    String getClusterJson(String ambariIp, Long stackId);

    String getClusterJson(String ambariIp, String stackName);

    void updateHosts(Long stackId, Set<HostGroupAdjustmentJson> hostGroupAdjustments);

    void updateStatus(Long stackId, StatusRequest statusRequest);
}
