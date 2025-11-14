package com.sequenceiq.cloudbreak.cluster.api;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.sequenceiq.cloudbreak.cloud.model.HostName;
import com.sequenceiq.cloudbreak.cluster.model.ClusterManagerCommand;
import com.sequenceiq.cloudbreak.cluster.service.ClusterClientInitException;
import com.sequenceiq.cloudbreak.cluster.status.ClusterStatusResult;
import com.sequenceiq.cloudbreak.cluster.status.ExtendedHostStatuses;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterCommandType;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;
import com.sequenceiq.cloudbreak.polling.ExtendedPollingResult;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;

public interface ClusterStatusService {

    /**
     * Determine cluster-level status based on service/component states.
     */
    ClusterStatusResult getStatus(boolean blueprintPresent);

    ExtendedHostStatuses getExtendedHostStatuses(Optional<String> runtimeVersion);

    List<String> getDecommissionedHostsFromCM();

    Map<HostName, String> getHostStatusesRaw();

    boolean isClusterManagerRunning();

    boolean isClusterManagerRunningQuickCheck();

    boolean isServiceRunningByType(String clusterName, String serviceType);

    Optional<String> getClusterManagerVersion();

    List<ClusterManagerCommand> getActiveCommandsList();

    Optional<ClusterManagerCommand> findCommand(StackDtoDelegate stack, ClusterCommandType command);

    boolean waitForHealthyServices(Optional<String> runtimeVersion);

    ExtendedPollingResult waitForHostHealthyServices(Set<InstanceMetadataView> hostsInCluster,
            Optional<String> runtimeVersion) throws ClusterClientInitException;

    String getDeployment(Stack stack);
}
