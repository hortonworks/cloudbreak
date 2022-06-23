package com.sequenceiq.cloudbreak.cluster.model.stopstart;

import java.util.Set;

import com.sequenceiq.cloudbreak.common.type.HealthCheckResult;

/**
 * DetailedServicesHealthCheck maintains information about all the services with BAD or CONCERNING health on the cluster.
 */
public class DetailedServicesHealthCheck {

    private final HealthCheckResult healthCheckResult;

    private final Set<String> servicesWithBadHealth;

    public DetailedServicesHealthCheck(HealthCheckResult healthCheckResult, Set<String> servicesWithBadHealth) {
        this.healthCheckResult = healthCheckResult;
        this.servicesWithBadHealth = servicesWithBadHealth;
    }

    public Set<String> getServicesWithBadHealth() {
        return servicesWithBadHealth;
    }

    public HealthCheckResult getHealthCheckResult() {
        return healthCheckResult;
    }

    @Override
    public String toString() {
        return "DetailedServicesHealthCheck{" +
                "servicesWithBadHealth=" + servicesWithBadHealth +
                '}';
    }
}
