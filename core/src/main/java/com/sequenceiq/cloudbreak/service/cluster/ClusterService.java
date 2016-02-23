package com.sequenceiq.cloudbreak.service.cluster;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sequenceiq.cloudbreak.api.model.ClusterResponse;
import com.sequenceiq.cloudbreak.api.model.HostGroupAdjustmentJson;
import com.sequenceiq.cloudbreak.api.model.Status;
import com.sequenceiq.cloudbreak.api.model.StatusRequest;
import com.sequenceiq.cloudbreak.api.model.UserNamePasswordJson;
import com.sequenceiq.cloudbreak.core.CloudbreakSecuritySetupException;
import com.sequenceiq.cloudbreak.domain.AmbariStackDetails;
import com.sequenceiq.cloudbreak.domain.CbUser;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.HostGroup;
import com.sequenceiq.cloudbreak.service.cluster.event.ClusterStatusUpdateRequest;
import com.sequenceiq.cloudbreak.service.cluster.event.UpdateAmbariHostsRequest;
import com.sequenceiq.cloudbreak.service.stack.flow.HttpClientConfig;

public interface ClusterService {

    Cluster create(CbUser user, Long stackId, Cluster clusterRequest);

    void delete(CbUser user, Long stackId);

    Cluster retrieveClusterByStackId(Long stackId);

    ClusterResponse retrieveClusterForCurrentUser(Long stackId);

    Cluster updateAmbariClientConfig(Long clusterId, HttpClientConfig ambariClientConfig);

    void updateHostCountWithAdjustment(Long clusterId, String hostGroupName, Integer adjustment);

    void updateHostMetadata(Long clusterId, Map<String, List<String>> hostsPerHostGroup);

    String getClusterJson(String ambariIp, Long stackId);

    UpdateAmbariHostsRequest updateHosts(Long stackId, HostGroupAdjustmentJson hostGroupAdjustment) throws CloudbreakSecuritySetupException;

    ClusterStatusUpdateRequest updateStatus(Long stackId, StatusRequest statusRequest);

    Cluster updateClusterStatusByStackId(Long stackId, Status status, String statusReason);

    Cluster updateClusterStatusByStackId(Long stackId, Status status);

    Cluster updateCluster(Cluster cluster);

    Cluster updateClusterMetadata(Long stackId);

    Cluster updateClusterUsernameAndPassword(Cluster cluster, String userName, String password);

    Cluster recreate(Long stackId, Long blueprintId, Set<HostGroup> hostGroups, boolean validateBlueprint, AmbariStackDetails ambariStackDetails);

    Cluster updateUserNamePassword(Long stackId, UserNamePasswordJson userNamePasswordJson);

    ClusterResponse getClusterResponse(ClusterResponse response, String clusterJson);

    Cluster getById(Long clusterId);
}
