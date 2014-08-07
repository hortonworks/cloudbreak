package com.sequenceiq.cloudbreak.service.cluster;

import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.User;

public interface ClusterService {
    void createCluster(User user, Long stackId, Cluster clusterRequest);

    Cluster retrieveCluster(User user, Long stackId);

    void startAllService(User user, Long stackId);

    void stopAllService(User user, Long stackId);

    String getClusterJson(String ambariIp, Long stackId);

}
