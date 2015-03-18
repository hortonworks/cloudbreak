package com.sequenceiq.cloudbreak.service.cluster;

import com.sequenceiq.cloudbreak.controller.json.HostGroupAdjustmentJson;
import com.sequenceiq.cloudbreak.domain.CbUser;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.Status;
import com.sequenceiq.cloudbreak.domain.StatusRequest;
import com.sequenceiq.cloudbreak.service.cluster.event.ClusterStatusUpdateRequest;
import com.sequenceiq.cloudbreak.service.cluster.event.UpdateAmbariHostsRequest;

public interface ClusterService {

    Cluster create(CbUser user, Long stackId, Cluster clusterRequest);

    Cluster retrieveCluster(Long stackId);

    String getClusterJson(String ambariIp, Long stackId);

    UpdateAmbariHostsRequest updateHosts(Long stackId, HostGroupAdjustmentJson hostGroupAdjustment);

    ClusterStatusUpdateRequest updateStatus(Long stackId, StatusRequest statusRequest);

    Cluster clusterCreationSuccess(Long clusterId, long creationFinished, String ambariIp);

    Cluster updateClusterStatus(Long clusterId, Status status, String statusReason);
}
