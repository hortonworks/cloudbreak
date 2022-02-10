package com.sequenceiq.cloudbreak.cluster.api;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.sequenceiq.cloudbreak.cloud.model.HostName;
import com.sequenceiq.cloudbreak.cluster.model.ClusterManagerCommand;
import com.sequenceiq.cloudbreak.cluster.status.ClusterStatusResult;
import com.sequenceiq.cloudbreak.cluster.status.ExtendedHostStatuses;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterCommandType;

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

    Optional<String> getClusterManagerVersion();

    List<String> getActiveCommandsList();

    Optional<ClusterManagerCommand> findCommand(Stack stack, ClusterCommandType command);
}
