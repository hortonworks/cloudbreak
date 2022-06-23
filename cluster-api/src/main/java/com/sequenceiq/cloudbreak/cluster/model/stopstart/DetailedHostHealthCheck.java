package com.sequenceiq.cloudbreak.cluster.model.stopstart;

import com.sequenceiq.cloudbreak.common.type.HealthCheckResult;

/**
 * DetailedHostHealthCheck maintains information about maintenanceMode, commissionState and healthCheck data retrieved by CM for the host
 */
public class DetailedHostHealthCheck {
    private final HealthCheckResult healthCheckResult;

    private final boolean inMaintenanceMode;

    private final HostCommissionState hostCommissionState;

    private final String lastHeartbeat;

    public DetailedHostHealthCheck(HealthCheckResult healthCheckResult, boolean inMaintenanceMode,
            HostCommissionState hostCommissionState, String lastHeartbeat) {
        this.healthCheckResult = healthCheckResult;
        this.inMaintenanceMode = inMaintenanceMode;
        this.hostCommissionState = hostCommissionState;
        this.lastHeartbeat = lastHeartbeat;
    }

    public HealthCheckResult getHealthCheckResult() {
        return healthCheckResult;
    }

    public boolean isInMaintenanceMode() {
        return inMaintenanceMode;
    }

    public HostCommissionState getHostCommissionState() {
        return hostCommissionState;
    }

    public String getLastHeartbeat() {
        return lastHeartbeat;
    }

    @Override
    public String toString() {
        return "DetailedHostHealthCheck{" +
                "healthCheckResult=" + healthCheckResult +
                ", inMaintenanceMode=" + inMaintenanceMode +
                ", hostCommissionState=" + hostCommissionState +
                ", lastHeartbeat=" + lastHeartbeat +
                '}';
    }
}
