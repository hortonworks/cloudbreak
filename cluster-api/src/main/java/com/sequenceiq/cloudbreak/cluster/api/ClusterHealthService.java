package com.sequenceiq.cloudbreak.cluster.api;

import com.sequenceiq.cloudbreak.cluster.status.DetailedHostStatuses;

/**
 * Determine cluster host health info from CM APIs
 */
public interface ClusterHealthService {

    DetailedHostStatuses getDetailedHostStatuses();
}
