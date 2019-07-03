package com.sequenceiq.periscope.model;

import com.sequenceiq.periscope.domain.ClusterManager;
import com.sequenceiq.periscope.domain.SecurityConfig;

public final class MonitoredStack {

    private final ClusterManager clusterManager;

    private final Long stackId;

    private final SecurityConfig securityConfig;

    public MonitoredStack(ClusterManager clusterManager) {
        this(clusterManager, null, null);
    }

    public MonitoredStack(ClusterManager clusterManager, Long stackId, SecurityConfig securityConfig) {
        this.clusterManager = clusterManager;
        this.stackId = stackId;
        this.securityConfig = securityConfig;
    }

    public ClusterManager getClusterManager() {
        return clusterManager;
    }

    public Long getStackId() {
        return stackId;
    }

    public SecurityConfig getSecurityConfig() {
        return securityConfig;
    }
}
