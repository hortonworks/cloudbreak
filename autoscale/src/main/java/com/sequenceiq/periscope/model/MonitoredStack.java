package com.sequenceiq.periscope.model;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.periscope.domain.ClusterManager;
import com.sequenceiq.periscope.domain.SecurityConfig;

public final class MonitoredStack {

    private final ClusterManager clusterManager;

    private final String stackCrn;

    private final String stackName;

    private final StackType stackType;

    private final String cloudPlatform;

    private final Long stackId;

    private final SecurityConfig securityConfig;

    private final Tunnel tunnel;

    @SuppressWarnings("checkstyle:ExecutableStatementCount")
    public MonitoredStack(ClusterManager clusterManager, String stackName, String stackCrn, String cloudPlatform,
            StackType stackType, Long stackId, SecurityConfig securityConfig, Tunnel tunnel) {
        this.clusterManager = clusterManager;
        this.securityConfig = securityConfig;
        this.tunnel = tunnel;
        this.stackId = stackId;
        this.stackName = stackName;
        this.stackCrn = stackCrn;
        this.cloudPlatform = cloudPlatform;
        this.stackType = stackType;
    }

    public ClusterManager getClusterManager() {
        return clusterManager;
    }

    public String getStackCrn() {
        return stackCrn;
    }

    public String getStackName() {
        return stackName;
    }

    public StackType getStackType() {
        return stackType;
    }

    public String getCloudPlatform() {
        return cloudPlatform;
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
