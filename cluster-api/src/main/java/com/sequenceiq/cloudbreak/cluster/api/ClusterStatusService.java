package com.sequenceiq.cloudbreak.cluster.api;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.sequenceiq.cloudbreak.cloud.model.HostName;
import com.sequenceiq.cloudbreak.cluster.status.ClusterStatusResult;
import com.sequenceiq.cloudbreak.cluster.status.ExtendedHostStatuses;

public interface ClusterStatusService {

    /**
     * Determine cluster-level status based on service/component states.
     */
    ClusterStatusResult getStatus(boolean blueprintPresent);

    ExtendedHostStatuses getExtendedHostStatuses(Optional<String> runtimeVersion);

    Map<HostName, String> getHostStatusesRaw();

    boolean isClusterManagerRunning();

    boolean isClusterManagerRunningQuickCheck();

    Optional<String> getClusterManagerVersion();

    List<String> getActiveCommandsList();
}
