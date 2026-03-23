package com.sequenceiq.cloudbreak.cluster.status;

import static com.sequenceiq.cloudbreak.cluster.util.HostnameTransformer.getHostnamePatterns;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Joiner;
import com.sequenceiq.cloudbreak.cloud.model.HostName;
import com.sequenceiq.cloudbreak.common.type.HealthCheck;
import com.sequenceiq.cloudbreak.common.type.HealthCheckResult;
import com.sequenceiq.cloudbreak.common.type.HealthCheckType;

public class DefaultUnhealthyReasonProvider implements UnhealthyReasonProvider {

    private final HealthCheckType healthCheckType;

    public DefaultUnhealthyReasonProvider(HealthCheckType healthCheckType) {
        this.healthCheckType = healthCheckType;
    }

    @Override
    public String getReason(Map<HostName, Set<HealthCheck>> hostsHealth) {
        Map<String, List<String>> hostNamesByHealthCheckDetails = hostsHealth.entrySet().stream()
                .flatMap(hostNameSetEntry -> getUnhealthyHostHealthCheckDetails(hostNameSetEntry).stream())
                .collect(Collectors.groupingBy(HostNameAndDetails::details, Collectors.mapping(HostNameAndDetails::hostname, Collectors.toList())));
        return hostNamesByHealthCheckDetails
                .entrySet()
                .stream()
                .map(entry -> getHostnamePatterns(entry.getValue()) + ": " + entry.getKey())
                .collect(Collectors.joining(". "));
    }

    private Optional<HostNameAndDetails> getUnhealthyHostHealthCheckDetails(Map.Entry<HostName, Set<HealthCheck>> hostNameSetEntry) {
        String details = hostNameSetEntry.getValue().stream()
                .filter(healthCheck -> healthCheck.getType() == healthCheckType)
                .filter(healthCheck -> HealthCheckResult.UNHEALTHY.equals(healthCheck.getResult()))
                .map(healthCheck -> healthCheck.getReason().map(reason -> reason + ": ").orElse("") + Joiner.on(", ").join(healthCheck.getDetails()))
                .collect(Collectors.joining(", "));
        return StringUtils.isNotBlank(details)
                ? Optional.of(new HostNameAndDetails(hostNameSetEntry.getKey().value(), details))
                : Optional.empty();
    }

    private record HostNameAndDetails(String hostname, String details) {

    }
}

