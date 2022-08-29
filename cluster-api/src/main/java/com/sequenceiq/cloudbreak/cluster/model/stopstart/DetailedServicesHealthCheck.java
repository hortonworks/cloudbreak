package com.sequenceiq.cloudbreak.cluster.model.stopstart;

import java.util.Set;

/**
 * DetailedServicesHealthCheck maintains information about all the services with BAD or CONCERNING health on the cluster.
 */
public class DetailedServicesHealthCheck {

    private final Set<String> servicesWithBadHealth;

    private final Set<String> servicesNotRunning;

    public DetailedServicesHealthCheck(Set<String> servicesWithBadHealth, Set<String> servicesNotRunning) {
        this.servicesWithBadHealth = servicesWithBadHealth;
        this.servicesNotRunning = servicesNotRunning;
    }

    public Set<String> getServicesWithBadHealth() {
        return servicesWithBadHealth;
    }

    public Set<String> getServicesNotRunning() {
        return servicesNotRunning;
    }

    @Override
    public String toString() {
        return "DetailedServicesHealthCheck{" +
                "servicesWithBadHealth=" + servicesWithBadHealth +
                "servicesNotRunning=" + servicesNotRunning +
                '}';
    }
}
