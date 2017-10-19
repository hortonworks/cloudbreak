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
import com.sequenceiq.cloudbreak.cloud.model.AmbariRepo;
import com.sequenceiq.cloudbreak.cloud.model.component.StackRepoDetails;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.common.type.HostMetadataState;
import com.sequenceiq.cloudbreak.core.CloudbreakSecuritySetupException;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.ClusterComponent;
import com.sequenceiq.cloudbreak.domain.HostGroup;
import com.sequenceiq.cloudbreak.domain.Stack;

public interface ClusterService {

    Cluster create(IdentityUser user, Stack stack, Cluster clusterRequest, List<ClusterComponent> component);

    void delete(Long stackId);

    Cluster retrieveClusterByStackId(Long stackId);

    <R extends ClusterResponse> R retrieveClusterForCurrentUser(Long stackId, Class<R> clazz);

    Cluster updateAmbariClientConfig(Long clusterId, HttpClientConfig ambariClientConfig);

    void updateHostCountWithAdjustment(Long clusterId, String hostGroupName, Integer adjustment);

    void updateHostMetadata(Long clusterId, Map<String, List<String>> hostsPerHostGroup, HostMetadataState hostMetadataState);

    String getClusterJson(String ambariIp, Long stackId);

    void updateHosts(Long stackId, HostGroupAdjustmentJson hostGroupAdjustment) throws CloudbreakSecuritySetupException;

    void updateStatus(Long stackId, StatusRequest statusRequest);

    Cluster updateClusterStatusByStackId(Long stackId, Status status, String statusReason);

    Cluster updateClusterStatusByStackId(Long stackId, Status status);

    Cluster updateClusterStatusByStackIdOutOfTransaction(Long stackId, Status status);

    Cluster updateCluster(Cluster cluster);

    Cluster updateClusterMetadata(Long stackId);

    Cluster recreate(Long stackId, Long blueprintId, Set<HostGroup> hostGroups, boolean validateBlueprint, StackRepoDetails stackRepoDetails);

    void updateUserNamePassword(Long stackId, UserNamePasswordJson userNamePasswordJson);

    <R extends ClusterResponse> R getClusterResponse(R response, String clusterJson);

    Cluster getById(Long clusterId);

    ConfigsResponse retrieveOutputs(Long stackId, Set<BlueprintParameterJson> requests) throws CloudbreakSecuritySetupException, IOException;

    void upgrade(Long stackId, AmbariRepo ambariRepo);

    Map<String, String> getHostStatuses(Long stackId) throws CloudbreakSecuritySetupException;

    void failureReport(Long stackId, List<String> failedNodes) throws CloudbreakSecuritySetupException;

    void repairCluster(Long stackId, List<String> hostGroups, boolean removeOnly);
}
