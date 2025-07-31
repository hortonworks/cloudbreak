package com.sequenceiq.cloudbreak.cm;

import static com.sequenceiq.cloudbreak.cloud.model.HostName.hostName;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.collections4.CollectionUtils.emptyIfNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.cloudera.api.swagger.HostsResourceApi;
import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiHealthCheck;
import com.cloudera.api.swagger.model.ApiHealthSummary;
import com.cloudera.api.swagger.model.ApiHost;
import com.cloudera.api.swagger.model.ApiHostList;
import com.cloudera.api.swagger.model.ApiRoleRef;
import com.google.api.client.util.Maps;
import com.google.common.base.Joiner;
import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.cloud.model.HostName;
import com.sequenceiq.cloudbreak.cluster.status.ExtendedHostStatuses;
import com.sequenceiq.cloudbreak.cm.client.retry.ClouderaManagerApiFactory;
import com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil;
import com.sequenceiq.cloudbreak.common.type.HealthCheck;
import com.sequenceiq.cloudbreak.common.type.HealthCheckResult;
import com.sequenceiq.cloudbreak.common.type.HealthCheckType;

@Service
public class ClouderaManagerHealthService {

    public static final String HOST_SCM_HEALTH = "HOST_SCM_HEALTH";

    public static final Set<ApiHealthSummary> IGNORED_HEALTH_SUMMARIES = Sets.immutableEnumSet(
            ApiHealthSummary.DISABLED,
            ApiHealthSummary.NOT_AVAILABLE,
            ApiHealthSummary.HISTORY_NOT_AVAILABLE
    );

    public static final String FULL_WITH_EXPLANATION_VIEW = "FULL_WITH_HEALTH_CHECK_EXPLANATION";

    public static final String HOST_AGENT_CERTIFICATE_EXPIRY = "HOST_AGENT_CERTIFICATE_EXPIRY";

    private static final String FULL_VIEW = "FULL";

    private static final Logger LOGGER = LoggerFactory.getLogger(ClouderaManagerHealthService.class);

    private static final String DEFAULT_STATUS_REASON = "Cloudera Manager reported bad health for this host.";

    private static final String MAINTENANCE_MODE = "This host is in maintenance mode.";

    private static final String DUPLICATED_HOST = "This host is duplicated in Cloudera Manager, please check it in Cloudera Manager.";

    private static final Set<Function<ApiHost, Optional<HealthCheck>>> HEALTH_CHECK_FUNCTIONS = Sets.newHashSet(
            ClouderaManagerHealthService::getHostHealthCheck,
            ClouderaManagerHealthService::getServicesHealthCheck,
            ClouderaManagerHealthService::getCertCheck
    );

    @Inject
    private ClouderaManagerApiFactory clouderaManagerApiFactory;

    private static Optional<HealthCheck> getServicesHealthCheck(ApiHost host) {
        Set<String> servicesWithBadHealth = collectServicesWithBadHealthOnHost(host);
        if (!servicesWithBadHealth.isEmpty()) {
            String statusReason = String.format("The following services are in bad health: %s.", Joiner.on(", ").join(servicesWithBadHealth));
            return Optional.of(new HealthCheck(HealthCheckType.SERVICES, HealthCheckResult.UNHEALTHY, Optional.of(statusReason), Optional.empty()));
        }
        return Optional.empty();
    }

    private static Set<String> collectServicesWithBadHealthOnHost(ApiHost host) {
        return emptyIfNull(host.getRoleRefs()).stream()
                .filter(roleRef -> ApiHealthSummary.BAD.equals(roleRef.getHealthSummary()))
                .map(ApiRoleRef::getServiceName)
                .collect(Collectors.toSet());
    }

    private static Optional<HealthCheck> getCertCheck(ApiHost apiHost) {
        Optional<ApiHealthCheck> healthCheck = emptyIfNull(apiHost.getHealthChecks()).stream()
                .filter(health -> HOST_AGENT_CERTIFICATE_EXPIRY.equals(health.getName()))
                .findFirst();
        if (healthCheck.isPresent()) {
            HealthCheckResult result = ApiHealthSummary.BAD.equals(healthCheck.get().getSummary())
                    || ApiHealthSummary.CONCERNING.equals(healthCheck.get().getSummary()) ? HealthCheckResult.UNHEALTHY : HealthCheckResult.HEALTHY;
            Optional<String> reason = Optional.ofNullable(healthCheck.get().getSummary()).map(apiSum -> "Cert health on CM: " + apiSum.getValue());
            Optional<String> details = result == HealthCheckResult.UNHEALTHY ? Optional.ofNullable(healthCheck.get().getExplanation()) : Optional.empty();
            return Optional.of(new HealthCheck(HealthCheckType.CERT, result, reason, details));
        }
        return Optional.empty();
    }

    private static Optional<HealthCheck> getHostHealthCheck(ApiHost apiHost) {
        return emptyIfNull(apiHost.getHealthChecks()).stream()
                .filter(health -> HOST_SCM_HEALTH.equals(health.getName()))
                .filter(health -> !IGNORED_HEALTH_SUMMARIES.contains(health.getSummary()))
                .findFirst()
                .map(apiHealthCheck -> new HealthCheck(
                        HealthCheckType.HOST,
                        healthSummaryToHealthCheckResult(apiHealthCheck.getSummary(), apiHost.getMaintenanceMode()),
                        getHostHealthMessage(apiHealthCheck.getSummary(), apiHealthCheck.getExplanation(), apiHost.getMaintenanceMode()), Optional.empty()));
    }

    private static Optional<String> getHostHealthMessage(ApiHealthSummary healthSummary, String explanation, Boolean maintenanceMode) {
        if (Boolean.TRUE.equals(maintenanceMode)) {
            return Optional.of(MAINTENANCE_MODE);
        }
        if (healthSummaryToHealthCheckResult(healthSummary, false) == HealthCheckResult.UNHEALTHY) {
            if (StringUtils.isNotBlank(explanation)) {
                return Optional.of(explanation.endsWith(".") ? explanation : explanation + ".");
            } else {
                return Optional.of(DEFAULT_STATUS_REASON);
            }
        }
        return Optional.empty();
    }

    private static HealthCheckResult healthSummaryToHealthCheckResult(ApiHealthSummary healthSummary, Boolean maintenanceMode) {
        if (Boolean.TRUE.equals(maintenanceMode)) {
            return HealthCheckResult.UNHEALTHY;
        }
        switch (healthSummary) {
            case GOOD:
            case CONCERNING:
                return HealthCheckResult.HEALTHY;
            default:
                return HealthCheckResult.UNHEALTHY;
        }
    }

    private static <T> Map<HostName, T> convertHealthSummary(Map<HostName, ApiHealthSummary> hostHealth, Function<ApiHealthSummary, T> converter) {
        Map<HostName, T> result = new HashMap<>();
        hostHealth.forEach((hostname, healthSummary) -> result.put(hostname, converter.apply(healthSummary)));
        return result;
    }

    public ExtendedHostStatuses getExtendedHostStatuses(ApiClient client, Optional<String> runtimeVersion) {
        List<ApiHost> apiHostList = getHostsFromCM(client);
        boolean cmServicesHealthCheckAllowed = CMRepositoryVersionUtil.isCmServicesHealthCheckAllowed(runtimeVersion);
        Map<HostName, Set<HealthCheck>> hostStates = Maps.newHashMap();
        apiHostList.forEach(apiHost -> hostStates.merge(hostName(apiHost.getHostname()), getHealthChecks(apiHost, cmServicesHealthCheckAllowed),
                (k, v) -> Set.of(new HealthCheck(HealthCheckType.HOST, HealthCheckResult.UNHEALTHY, Optional.of(DUPLICATED_HOST), Optional.empty()))));
        hostStates.entrySet().removeIf(entry -> entry.getValue().isEmpty());
        LOGGER.debug("Creating 'ExtendedHostStatuses' with {}", hostStates);
        return new ExtendedHostStatuses(hostStates);
    }

    public Map<HostName, String> getHostStatusesRaw(ApiClient client) {
        return convertHealthSummary(getHostHealthSummary(client), ApiHealthSummary::getValue);
    }

    private List<ApiHost> getHostsFromCM(ApiClient client) {
        HostsResourceApi api = clouderaManagerApiFactory.getHostsResourceApi(client);
        try {
            ApiHostList apiHostList = api.readHosts(null, null, FULL_WITH_EXPLANATION_VIEW);
            LOGGER.trace("Response from CM for readHosts call: {}", apiHostList);
            return apiHostList.getItems();
        } catch (ApiException e) {
            LOGGER.info("Failed to get hosts from CM", e);
            throw new RuntimeException("Failed to get hosts from CM due to: " + e.getMessage(), e);
        }
    }

    private Set<HealthCheck> getHealthChecks(ApiHost apiHost, boolean cmServicesHealthCheckAllowed) {
        Set<HealthCheck> healthChecks = HEALTH_CHECK_FUNCTIONS.stream()
                .map(healthCheck -> healthCheck.apply(apiHost))
                .flatMap(Optional::stream)
                .collect(Collectors.toSet());
        healthChecks.removeIf(healthCheck -> HealthCheckType.SERVICES.equals(healthCheck.getType()) && !cmServicesHealthCheckAllowed);
        return healthChecks;
    }

    /**
     * Collects summary of HOST_SCM_HEALTH check for each host.
     * Currently this is the best indicator of host availability.
     */
    private Map<HostName, ApiHealthSummary> getHostHealthSummary(ApiClient client) {
        HostsResourceApi api = clouderaManagerApiFactory.getHostsResourceApi(client);
        try {
            return api.readHosts(null, null, FULL_VIEW).getItems().stream()
                    .filter(host -> host.getHealthChecks() != null)
                    .flatMap(host -> host.getHealthChecks().stream()
                            .filter(check -> ClouderaManagerHealthService.HOST_SCM_HEALTH.equals(check.getName()))
                            .map(ApiHealthCheck::getSummary)
                            .filter(healthSummary -> !ClouderaManagerHealthService.IGNORED_HEALTH_SUMMARIES.contains(healthSummary))
                            .map(healthSummary -> Pair.of(hostName(host.getHostname()), healthSummary))
                    )
                    .collect(toMap(Pair::getLeft, Pair::getRight));
        } catch (ApiException e) {
            LOGGER.info("Failed to get hosts from CM", e);
            throw new RuntimeException("Failed to get hosts from CM due to: " + e.getMessage(), e);
        }
    }
}
