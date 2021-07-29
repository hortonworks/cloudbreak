package com.sequenceiq.cloudbreak.cluster.status;

import static org.apache.commons.collections4.CollectionUtils.emptyIfNull;

import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.sequenceiq.cloudbreak.cloud.model.HostName;
import com.sequenceiq.cloudbreak.common.type.HealthCheck;
import com.sequenceiq.cloudbreak.common.type.HealthCheckResult;
import com.sequenceiq.cloudbreak.common.type.HealthCheckType;

public class ExtendedHostStatuses {

    private final Map<HostName, Set<HealthCheck>> hostsHealth;

    public ExtendedHostStatuses(Map<HostName, Set<HealthCheck>> hostsHealth) {
        this.hostsHealth = hostsHealth;
    }

    public boolean isAnyCertExpiring() {
        Predicate<HealthCheck> certCheckPredicate = healthCheck -> HealthCheckType.CERT.equals(healthCheck.getType());
        return hostsHealth.values().stream()
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .filter(certCheckPredicate)
                .anyMatch(certHealth -> HealthCheckResult.UNHEALTHY.equals(certHealth.getResult()));
    }

    private boolean isHostUnhealthy(HostName hostName) {
        return emptyIfNull(hostsHealth.get(hostName)).stream()
                .filter(healthCheck -> HealthCheckType.HOST.equals(healthCheck.getType()))
                .anyMatch(healthCheck -> HealthCheckResult.UNHEALTHY.equals(healthCheck.getResult()));
    }

    private boolean hasHostUnhealthyServices(HostName hostName) {
        return emptyIfNull(hostsHealth.get(hostName)).stream()
                .filter(healthCheck -> HealthCheckType.SERVICES.equals(healthCheck.getType()))
                .anyMatch(healthCheck -> HealthCheckResult.UNHEALTHY.equals(healthCheck.getResult()));
    }

    public boolean isHostHealthy(HostName hostName) {
        return !isHostUnhealthy(hostName) && !hasHostUnhealthyServices(hostName);
    }

    public String statusReasonForHost(HostName hostName) {
        return emptyIfNull(hostsHealth.get(hostName)).stream()
                .sorted(Comparator.comparing(HealthCheck::getType))
                .map(HealthCheck::getReason)
                .flatMap(Optional::stream)
                .collect(Collectors.joining(" "));
    }

    public Map<HostName, Set<HealthCheck>> getHostsHealth() {
        return hostsHealth;
    }

    @Override
    public String toString() {
        return "ExtendedHostStatuses{" +
                "hostHealth=" + hostsHealth +
                '}';
    }
}
