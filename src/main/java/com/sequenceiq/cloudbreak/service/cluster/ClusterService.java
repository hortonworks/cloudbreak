package com.sequenceiq.cloudbreak.service.cluster;

import java.util.Map;

import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.StatusRequest;
import com.sequenceiq.cloudbreak.domain.User;

public interface ClusterService {
    void createCluster(User user, Long stackId, Cluster clusterRequest);

    Cluster retrieveCluster(User user, Long stackId);

    void updateStatus(User user, Long stackId, StatusRequest statusRequest);

    String getClusterJson(String ambariIp, Long stackId);

    void updateHosts(User user, Long stackId, Map<String, Integer> hosts);
}
