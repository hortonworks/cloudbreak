package com.sequenceiq.cloudbreak.cluster.status;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.sequenceiq.cloudbreak.cloud.model.HostName;
import com.sequenceiq.cloudbreak.cluster.model.stopstart.DetailedCertHealthCheck;
import com.sequenceiq.cloudbreak.cluster.model.stopstart.DetailedHostHealthCheck;
import com.sequenceiq.cloudbreak.cluster.model.stopstart.DetailedServicesHealthCheck;
import com.sequenceiq.cloudbreak.cluster.model.stopstart.HostCommissionState;
import com.sequenceiq.cloudbreak.common.type.HealthCheckResult;

/**
 * DetailedHostStatuses keeps track of HOST (maintenanceMode, commissionStatus), SERVICES and CERTIFICATE health on the cluster using info retrieved via CM APIs
 */
public class DetailedHostStatuses {

    private final Map<HostName, Optional<DetailedHostHealthCheck>> hostsHealth;

    private final Map<HostName, Optional<DetailedServicesHealthCheck>> servicesHealth;

    private final Map<HostName, Optional<DetailedCertHealthCheck>> certHealth;

    public DetailedHostStatuses(Map<HostName, Optional<DetailedHostHealthCheck>> hostsHealth, Map<HostName,
            Optional<DetailedServicesHealthCheck>> servicesHealth, Map<HostName, Optional<DetailedCertHealthCheck>> certHealth) {
        this.hostsHealth = hostsHealth;
        this.servicesHealth = servicesHealth;
        this.certHealth = certHealth;
    }

    public boolean isHostUnHealthy(HostName hostName) {
        return hostsHealth.get(hostName).stream()
                .anyMatch(dhc -> HealthCheckResult.UNHEALTHY.equals(dhc.getHealthCheckResult()));
    }

    public boolean areServicesUnhealthy(HostName hostname) {
        return servicesHealth.get(hostname).stream()
                .anyMatch(dhc -> !dhc.getServicesWithBadHealth().isEmpty());
    }

    public boolean areServicesNotRunning(HostName hostname) {
        return servicesHealth.get(hostname).stream()
                .anyMatch(dhc -> !dhc.getServicesNotRunning().isEmpty());
    }

    public boolean areServicesIrrecoverable(HostName hostname, Set<String> irrecoverableHealthChecks) {
        Set<String> serviceRolesUnHealthy = servicesHealth.get(hostname).get().getServicesWithBadHealth();
        return serviceRolesUnHealthy.stream().anyMatch(irrecoverableHealthChecks::contains);
    }

    public boolean isCertExpiring(HostName hostName) {
        return certHealth.get(hostName).stream()
                .anyMatch(dhc -> HealthCheckResult.UNHEALTHY.equals(dhc.getHealthCheckResult()));
    }

    public boolean isHostHealthy(HostName hostName) {
        return !isHostUnHealthy(hostName)
                && !areServicesUnhealthy(hostName);
    }

    public boolean isHostInMaintenanceMode(HostName hostName) {
        return hostsHealth.get(hostName).stream()
                .anyMatch(DetailedHostHealthCheck::isInMaintenanceMode);
    }

    public boolean isHostDecommissioned(HostName hostName) {
        return hostsHealth.get(hostName).stream()
                .anyMatch(dhc -> HostCommissionState.DECOMMISSIONED.equals(dhc.getHostCommissionState()));
    }

    public Map<HostName, Optional<DetailedHostHealthCheck>> getHostsHealth() {
        return hostsHealth;
    }

    public Map<HostName, Optional<DetailedServicesHealthCheck>> getServicesHealth() {
        return servicesHealth;
    }

    public Map<HostName, Optional<DetailedCertHealthCheck>> getCertHealth() {
        return certHealth;
    }

    @Override
    public String toString() {
        return "DetailedHostStatuses{" +
                "hostsHealth=" + hostsHealth +
                ", servicesHealth=" + servicesHealth +
                ", certHealth=" + certHealth +
                '}';
    }
}
