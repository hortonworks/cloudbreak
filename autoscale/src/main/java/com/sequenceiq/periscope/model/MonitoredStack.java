package com.sequenceiq.periscope.model;

import com.sequenceiq.periscope.domain.ClusterManager;
import com.sequenceiq.periscope.domain.SecurityConfig;

public final class MonitoredStack {

    private final ClusterManager clusterManager;

    private final String stackCrn;

    private final Long stackId;

    private final SecurityConfig securityConfig;

    public MonitoredStack(ClusterManager clusterManager, Long stackId) {
        this(clusterManager, null, stackId, null);
    }

    public MonitoredStack(ClusterManager clusterManager, String stackCrn, Long stackId, SecurityConfig securityConfig) {
        this.clusterManager = clusterManager;
        this.stackCrn = stackCrn;
        this.stackId = stackId;
        this.securityConfig = securityConfig;
    }

    public ClusterManager getClusterManager() {
        return clusterManager;
    }

    public String getStackCrn() {
        return stackCrn;
    }

    public Long getStackId() {
        return stackId;
    }

    public SecurityConfig getSecurityConfig() {
        return securityConfig;
    }
}
