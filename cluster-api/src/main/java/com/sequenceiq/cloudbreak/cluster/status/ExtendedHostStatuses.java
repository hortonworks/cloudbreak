package com.sequenceiq.cloudbreak.cluster.status;

import static org.apache.commons.collections4.CollectionUtils.emptyIfNull;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.sequenceiq.cloudbreak.cloud.model.HostName;
import com.sequenceiq.cloudbreak.common.type.HealthCheck;
import com.sequenceiq.cloudbreak.common.type.HealthCheckResult;
import com.sequenceiq.cloudbreak.common.type.HealthCheckType;

public class ExtendedHostStatuses {

    private final Map<HostName, Set<HealthCheck>> hostsHealth;

    private final Map<HealthCheckType, UnhealthyReasonProvider> unhealthyReasonProviders;

    public ExtendedHostStatuses(Map<HostName, Set<HealthCheck>> hostsHealth) {
        this(hostsHealth, Map.of());
    }

    public ExtendedHostStatuses(Map<HostName, Set<HealthCheck>> hostsHealth,
            Map<HealthCheckType, UnhealthyReasonProvider> unhealthyReasonProviders) {
        this.hostsHealth = hostsHealth;
        this.unhealthyReasonProviders = Map.copyOf(unhealthyReasonProviders);
    }

    private boolean isHostUnhealthy(HostName hostName) {
        return emptyIfNull(hostsHealth.get(hostName)).stream()
                .filter(healthCheck -> HealthCheckType.HOST.equals(healthCheck.getType()))
                .anyMatch(healthCheck -> HealthCheckResult.UNHEALTHY.equals(healthCheck.getResult()));
    }

    public boolean hasHostUnhealthyServices(HostName hostName) {
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
                .filter(hc -> HealthCheckResult.UNHEALTHY == hc.getResult())
                .map(HealthCheck::getReason)
                .flatMap(Optional::stream)
                .collect(Collectors.joining(" "));
    }

    public Map<HostName, Set<HealthCheck>> getHostsHealth() {
        return hostsHealth;
    }

    public boolean isAnyUnhealthyWithType(HealthCheckType healthCheckType) {
        return hostsHealth.values().stream()
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .filter(healthCheck -> Objects.equals(healthCheckType, healthCheck.getType()))
                .anyMatch(certHealth -> HealthCheckResult.UNHEALTHY.equals(certHealth.getResult()));
    }

    public boolean isAnyUnhealthyOrMissingWithType(HealthCheckType healthCheckType) {
        return hostsHealth.values().stream()
                .filter(Objects::nonNull)
                .anyMatch(healthChecks -> {
                    List<HealthCheck> checksForType = healthChecks.stream()
                            .filter(Objects::nonNull)
                            .filter(hc -> Objects.equals(healthCheckType, hc.getType()))
                            .toList();
                    return checksForType.isEmpty() || checksForType.stream().anyMatch(hc -> HealthCheckResult.UNHEALTHY.equals(hc.getResult()));
                });
    }

    public String getUnhealthyReasonWithType(HealthCheckType healthCheckType) {
        return unhealthyReasonProviders.getOrDefault(healthCheckType, new DefaultUnhealthyReasonProvider(healthCheckType))
                .getReason(hostsHealth);
    }

    @Override
    public String toString() {
        return "ExtendedHostStatuses{" +
                "hostHealth=" + hostsHealth +
                '}';
    }
}