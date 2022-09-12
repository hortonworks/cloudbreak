package com.sequenceiq.cloudbreak.cluster.api;

import java.util.Optional;

import com.sequenceiq.cloudbreak.cluster.status.DetailedHostStatuses;

/**
 * Determine cluster host health info from CM APIs
 */
public interface ClusterHealthService {

    boolean isClusterManagerRunning();

    boolean checkRMhealth(Optional<String> runtimeVersion);

    DetailedHostStatuses getDetailedHostStatuses(Optional<String> runtimeVersion);
}
