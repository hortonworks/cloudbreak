package com.sequenceiq.cloudbreak.cluster.api;

import java.util.Map;

import com.sequenceiq.cloudbreak.cluster.status.ClusterStatusResult;
import com.sequenceiq.cloudbreak.common.type.ClusterManagerState;

public interface ClusterStatusService {

    /**
     * Determine cluster-level status based on service/component states.
     */
    ClusterStatusResult getStatus(boolean blueprintPresent);

    /**
     * Determine state of all hosts known by the Cluster Manager.
     */
    Map<String, ClusterManagerState.ClusterManagerStatus> getHostStatuses();

    Map<String, ClusterManagerState> getExtendedHostStatuses();

    Map<String, String> getHostStatusesRaw();
}
