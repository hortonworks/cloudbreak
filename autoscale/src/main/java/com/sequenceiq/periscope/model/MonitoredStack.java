package com.sequenceiq.periscope.model;

import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.periscope.domain.ClusterManager;
import com.sequenceiq.periscope.domain.SecurityConfig;

public final class MonitoredStack {

    private final ClusterManager clusterManager;

    private final String stackCrn;

    private final Long stackId;

    private final SecurityConfig securityConfig;

    private final Tunnel tunnel;

    public MonitoredStack(ClusterManager clusterManager, String stackCrn, Long stackId, SecurityConfig securityConfig, Tunnel tunnel) {
        this.clusterManager = clusterManager;
        this.stackCrn = stackCrn;
        this.stackId = stackId;
        this.securityConfig = securityConfig;
        this.tunnel = tunnel;
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

    public Tunnel getTunnel() {
        return tunnel;
    }
}
