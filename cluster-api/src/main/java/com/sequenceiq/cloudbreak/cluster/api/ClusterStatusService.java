package com.sequenceiq.cloudbreak.cluster.api;

import java.util.Map;
import java.util.Optional;

import com.sequenceiq.cloudbreak.cloud.model.HostName;
import com.sequenceiq.cloudbreak.cluster.status.ClusterStatusResult;
import com.sequenceiq.cloudbreak.cluster.status.ExtendedHostStatuses;
import com.sequenceiq.cloudbreak.common.type.ClusterManagerState;

public interface ClusterStatusService {

    /**
     * Determine cluster-level status based on service/component states.
     */
    ClusterStatusResult getStatus(boolean blueprintPresent);

    /**
     * Determine state of all hosts known by the Cluster Manager.
     */
    Map<HostName, ClusterManagerState.ClusterManagerStatus> getHostStatuses();

    ExtendedHostStatuses getExtendedHostStatuses();

    Map<HostName, String> getHostStatusesRaw();

    boolean isClusterManagerRunning();

    boolean isClusterManagerRunningQuickCheck();

    Optional<String> getClusterManagerVersion();

}
