package com.sequenceiq.cloudbreak.service.cluster;

import java.util.Set;

import com.sequenceiq.cloudbreak.controller.json.HostGroupAdjustmentJson;
import com.sequenceiq.cloudbreak.core.CloudbreakSecuritySetupException;
import com.sequenceiq.cloudbreak.domain.AmbariStackDetails;
import com.sequenceiq.cloudbreak.domain.CbUser;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.HostGroup;
import com.sequenceiq.cloudbreak.domain.Status;
import com.sequenceiq.cloudbreak.domain.StatusRequest;
import com.sequenceiq.cloudbreak.service.cluster.event.ClusterStatusUpdateRequest;
import com.sequenceiq.cloudbreak.service.cluster.event.UpdateAmbariHostsRequest;

public interface ClusterService {

    Cluster create(CbUser user, Long stackId, Cluster clusterRequest);

    Cluster retrieveClusterByStackId(Long stackId);

    Cluster retrieveClusterForCurrentUser(Long stackId);

    Cluster updateAmbariIp(Long clusterId, String ambariIp);

    String getClusterJson(String ambariIp, Long stackId);

    UpdateAmbariHostsRequest updateHosts(Long stackId, HostGroupAdjustmentJson hostGroupAdjustment) throws CloudbreakSecuritySetupException;

    ClusterStatusUpdateRequest updateStatus(Long stackId, StatusRequest statusRequest);

    Cluster updateClusterStatusByStackId(Long stackId, Status status, String statusReason);

    Cluster updateClusterStatusByStackId(Long stackId, Status status);

    Cluster updateCluster(Cluster cluster);

    Cluster updateClusterMetadata(Long stackId);

    Cluster recreate(Long stackId, Long blueprintId, Set<HostGroup> hostGroups, boolean validateBlueprint, AmbariStackDetails ambariStackDetails);
}
