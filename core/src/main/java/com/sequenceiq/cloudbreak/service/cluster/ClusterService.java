package com.sequenceiq.cloudbreak.service.cluster;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sequenceiq.cloudbreak.api.model.BlueprintParameterJson;
import com.sequenceiq.cloudbreak.api.model.ClusterResponse;
import com.sequenceiq.cloudbreak.api.model.ConfigsResponse;
import com.sequenceiq.cloudbreak.api.model.HostGroupAdjustmentJson;
import com.sequenceiq.cloudbreak.api.model.Status;
import com.sequenceiq.cloudbreak.api.model.StatusRequest;
import com.sequenceiq.cloudbreak.api.model.UserNamePasswordJson;
import com.sequenceiq.cloudbreak.client.HttpClientConfig;
import com.sequenceiq.cloudbreak.cloud.model.HDPRepo;
import com.sequenceiq.cloudbreak.core.CloudbreakSecuritySetupException;
import com.sequenceiq.cloudbreak.domain.CbUser;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.Component;
import com.sequenceiq.cloudbreak.domain.HostGroup;

public interface ClusterService {

    Cluster create(CbUser user, Long stackId, Cluster clusterRequest, List<Component> component);

    void delete(CbUser user, Long stackId);

    Cluster retrieveClusterByStackId(Long stackId);

    ClusterResponse retrieveClusterForCurrentUser(Long stackId);

    Cluster updateAmbariClientConfig(Long clusterId, HttpClientConfig ambariClientConfig);

    void updateHostCountWithAdjustment(Long clusterId, String hostGroupName, Integer adjustment);

    void updateHostMetadata(Long clusterId, Map<String, List<String>> hostsPerHostGroup);

    String getClusterJson(String ambariIp, Long stackId);

    void updateHosts(Long stackId, HostGroupAdjustmentJson hostGroupAdjustment) throws CloudbreakSecuritySetupException;

    void updateStatus(Long stackId, StatusRequest statusRequest);

    Cluster updateClusterStatusByStackId(Long stackId, Status status, String statusReason);

    Cluster updateClusterStatusByStackId(Long stackId, Status status);

    Cluster updateCluster(Cluster cluster);

    Cluster updateClusterMetadata(Long stackId);

    Cluster updateClusterUsernameAndPassword(Cluster cluster, String userName, String password);

    Cluster recreate(Long stackId, Long blueprintId, Set<HostGroup> hostGroups, boolean validateBlueprint, HDPRepo hdpRepo);

    Cluster updateUserNamePassword(Long stackId, UserNamePasswordJson userNamePasswordJson);

    ClusterResponse getClusterResponse(ClusterResponse response, String clusterJson);

    Cluster getById(Long clusterId);

    ConfigsResponse retrieveOutputs(Long stackId, Set<BlueprintParameterJson> requests) throws CloudbreakSecuritySetupException, IOException;
}
