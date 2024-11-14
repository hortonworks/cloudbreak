package com.sequenceiq.cloudbreak.cluster.api;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.sequenceiq.cloudbreak.cluster.status.DetailedHostStatuses;

/**
 * Determine cluster host health info from CM APIs
 */
public interface ClusterHealthService {

    boolean isClusterManagerRunning();

    Map<String, String> readServicesHealth(String clusterName);

    DetailedHostStatuses getDetailedHostStatuses(Optional<String> runtimeVersion);

    Set<String> getDisconnectedNodeManagers();
}
