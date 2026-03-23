package com.sequenceiq.cloudbreak.cluster.status;

import static com.sequenceiq.cloudbreak.cluster.util.HostnameTransformer.getHostnamePatterns;
import static org.apache.commons.collections4.CollectionUtils.emptyIfNull;

import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.HostName;
import com.sequenceiq.cloudbreak.common.type.HealthCheck;
import com.sequenceiq.cloudbreak.common.type.HealthCheckResult;
import com.sequenceiq.cloudbreak.common.type.HealthCheckType;

@Component
public class StaleServiceConfigUnhealthyReasonProvider implements UnhealthyReasonProvider {

    private static final String STALE_CONFIG_INTRO = "The following services are running with stale configurations: ";

    private static final String STALE_CONFIG_HINT = "Please redeploy client configurations or restart these services to apply the pending updates.";

    private static final Pattern HOSTNAME_BASE_PATTERN = Pattern.compile("(.*?)(?:\\[.*]|\\d+)?$");

    @Override
    public String getReason(Map<HostName, Set<HealthCheck>> hostsHealth) {
        Map<String, Set<String>> hostsByService = new HashMap<>();
        hostsHealth.forEach((hostName, healthChecks) -> emptyIfNull(healthChecks).stream()
                .filter(healthCheck -> HealthCheckType.SERVICE_CONFIG_STALENESS == healthCheck.getType())
                .filter(healthCheck -> HealthCheckResult.UNHEALTHY.equals(healthCheck.getResult()))
                .flatMap(healthCheck -> healthCheck.getDetails().stream())
                .filter(StringUtils::isNotBlank)
                .forEach(service -> hostsByService.computeIfAbsent(service, ignored -> new LinkedHashSet<>()).add(hostName.value())));

        List<ServicesAndHosts> groupedServicesAndHosts = hostsByService.entrySet().stream()
                .collect(Collectors.groupingBy(
                        entry -> new LinkedHashSet<>(entry.getValue()),
                        Collectors.mapping(Map.Entry::getKey, Collectors.toList())))
                .entrySet().stream()
                .map(entry -> new ServicesAndHosts(entry.getValue().stream().sorted().toList(), entry.getKey().stream().toList()))
                .sorted(Comparator.comparingInt((ServicesAndHosts value) -> value.hosts().size()).reversed()
                        .thenComparing(value -> value.services().getFirst()))
                .toList();

        if (groupedServicesAndHosts.isEmpty()) {
            return "";
        }

        String serviceAndHostGroups = humanReadableJoin(groupedServicesAndHosts.stream()
                .map(this::toServiceHostMessagePart)
                .toList());
        return STALE_CONFIG_INTRO + serviceAndHostGroups + ". " + STALE_CONFIG_HINT;
    }

    private String toServiceHostMessagePart(ServicesAndHosts servicesAndHosts) {
        return humanReadableJoin(servicesAndHosts.services()) + " (on "
                + humanReadableJoin(getHostnamePatternsInEncounterOrder(servicesAndHosts.hosts())) + ")";
    }

    private List<String> getHostnamePatternsInEncounterOrder(List<String> hosts) {
        List<String> patterns = getHostnamePatterns(hosts);
        return patterns.stream()
                .sorted(Comparator.comparingInt((String pattern) -> matchedHostCount(pattern, hosts)).reversed()
                        .thenComparingInt(pattern -> firstHostMatchIndex(pattern, hosts)))
                .toList();
    }

    private int matchedHostCount(String hostnamePattern, List<String> hosts) {
        String base = hostnameBase(hostnamePattern);
        return (int) hosts.stream().filter(host -> host.startsWith(base)).count();
    }

    private int firstHostMatchIndex(String hostnamePattern, List<String> hosts) {
        String base = hostnameBase(hostnamePattern);
        for (int i = 0; i < hosts.size(); i++) {
            if (hosts.get(i).startsWith(base)) {
                return i;
            }
        }
        return Integer.MAX_VALUE;
    }

    private String hostnameBase(String hostnamePattern) {
        Matcher matcher = HOSTNAME_BASE_PATTERN.matcher(hostnamePattern);
        return matcher.matches() ? matcher.group(1) : hostnamePattern;
    }

    private String humanReadableJoin(List<String> values) {
        if (values.isEmpty()) {
            return "";
        }
        if (values.size() == 1) {
            return values.getFirst();
        }
        if (values.size() == 2) {
            return values.get(0) + " and " + values.get(1);
        }
        return String.join(", ", values.subList(0, values.size() - 1)) + ", and " + values.get(values.size() - 1);
    }

    private record ServicesAndHosts(List<String> services, List<String> hosts) {

    }
}

