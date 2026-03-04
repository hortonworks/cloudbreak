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

import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Joiner;
import com.sequenceiq.cloudbreak.cloud.model.HostName;
import com.sequenceiq.cloudbreak.cluster.util.HostnameTransformer;
import com.sequenceiq.cloudbreak.common.type.HealthCheck;
import com.sequenceiq.cloudbreak.common.type.HealthCheckResult;
import com.sequenceiq.cloudbreak.common.type.HealthCheckType;

public class ExtendedHostStatuses {

    private final Map<HostName, Set<HealthCheck>> hostsHealth;

    public ExtendedHostStatuses(Map<HostName, Set<HealthCheck>> hostsHealth) {
        this.hostsHealth = hostsHealth;
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

    public String getUnhealthyReasonWithType(HealthCheckType healthCheckType) {
        Map<String, List<String>> hostNamesByHealthCheckDetails = hostsHealth.entrySet().stream()
                .flatMap(hostNameSetEntry -> getUnhealthyHostHealthCheckDetails(hostNameSetEntry, healthCheckType).stream())
                .collect(Collectors.groupingBy(HostNameAndDetails::details, Collectors.mapping(HostNameAndDetails::hostname, Collectors.toList())));
        return hostNamesByHealthCheckDetails
                .entrySet()
                .stream()
                .map(entry -> getHostnamePatterns(entry.getValue()) + ": " + entry.getKey())
                .collect(Collectors.joining(". "));
    }

    private String getHostnamePatterns(List<String> hostnames) {
        return '(' + Joiner.on(", ").join(HostnameTransformer.getHostnamePatterns(hostnames)) + ')';
    }

    private Optional<HostNameAndDetails> getUnhealthyHostHealthCheckDetails(
            Map.Entry<HostName, Set<HealthCheck>> hostNameSetEntry, HealthCheckType healthCheckType) {
        String details = hostNameSetEntry.getValue().stream()
                .filter(healthCheck -> healthCheck.getType() == healthCheckType)
                .filter(healthCheck -> HealthCheckResult.UNHEALTHY.equals(healthCheck.getResult()))
                .map(healthCheck -> healthCheck.getReason().map(reason -> reason + ": ").orElse("") + Joiner.on(", ").join(healthCheck.getDetails()))
                .collect(Collectors.joining(", "));
        return StringUtils.isNotBlank(details)
                ? Optional.of(new HostNameAndDetails(hostNameSetEntry.getKey().value(), details))
                : Optional.empty();
    }

    @Override
    public String toString() {
        return "ExtendedHostStatuses{" +
                "hostHealth=" + hostsHealth +
                '}';
    }

    private record HostNameAndDetails(String hostname, String details) {

    }
}
