package com.sequenceiq.cloudbreak.cm;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.cloudera.api.swagger.HostsResourceApi;
import com.cloudera.api.swagger.RolesResourceApi;
import com.cloudera.api.swagger.ServicesResourceApi;
import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiHealthCheck;
import com.cloudera.api.swagger.model.ApiHealthSummary;
import com.cloudera.api.swagger.model.ApiRole;
import com.cloudera.api.swagger.model.ApiRoleState;
import com.cloudera.api.swagger.model.ApiService;
import com.cloudera.api.swagger.model.ApiServiceState;
import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.client.HttpClientConfig;
import com.sequenceiq.cloudbreak.cluster.api.ClusterStatusService;
import com.sequenceiq.cloudbreak.cluster.status.ClusterStatus;
import com.sequenceiq.cloudbreak.cluster.status.ClusterStatusResult;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerClientFactory;
import com.sequenceiq.cloudbreak.common.type.HostMetadataState;
import com.sequenceiq.cloudbreak.domain.stack.Stack;

@Service
@Scope("prototype")
public class ClouderaManagerClusterStatusService implements ClusterStatusService {

    static final String HOST_SCM_HEALTH = "HOST_SCM_HEALTH";

    static final String FULL_VIEW = "FULL";

    private static final Logger LOGGER = LoggerFactory.getLogger(ClouderaManagerClusterStatusService.class);

    private static final Set<ApiServiceState> IGNORED_SERVICE_STATES = Sets.immutableEnumSet(
            ApiServiceState.NA,
            ApiServiceState.HISTORY_NOT_AVAILABLE
    );

    private static final Set<ApiRoleState> IGNORED_ROLE_STATES = Sets.immutableEnumSet(
            ApiRoleState.NA,
            ApiRoleState.HISTORY_NOT_AVAILABLE
    );

    private static final Set<ClusterStatus> NON_PENDING_STATES = Sets.immutableEnumSet(
            ClusterStatus.STARTED,
            ClusterStatus.INSTALLED
    );

    private static final Set<ApiHealthSummary> IGNORED_HEALTH_SUMMARIES = Sets.immutableEnumSet(
            ApiHealthSummary.DISABLED,
            ApiHealthSummary.NOT_AVAILABLE,
            ApiHealthSummary.HISTORY_NOT_AVAILABLE
    );

    @Inject
    private ClouderaManagerClientFactory clouderaManagerClientFactory;

    private final Stack stack;

    private final HttpClientConfig clientConfig;

    private ApiClient client;

    ClouderaManagerClusterStatusService(Stack stack, HttpClientConfig clientConfig) {
        this.stack = stack;
        this.clientConfig = clientConfig;
    }

    @PostConstruct
    public void initApiClient() {
        client = clouderaManagerClientFactory.getClient(stack, stack.getCluster(), clientConfig);
    }

    @Override
    public ClusterStatusResult getStatus(boolean blueprintPresent) {
        if (!isCMRunning()) {
            return ClusterStatusResult.of(ClusterStatus.AMBARISERVER_NOT_RUNNING);
        } else if (blueprintPresent) {
            return determineClusterStatus(stack);
        } else {
            return ClusterStatusResult.of(ClusterStatus.AMBARISERVER_RUNNING);
        }
    }

    @Override
    public Map<String, HostMetadataState> getHostStatuses() {
        return convertHealthSummary(getHostHealthSummary(), ClouderaManagerClusterStatusService::healthSummaryToState);
    }

    @Override
    public Map<String, String> getHostStatusesRaw() {
        return convertHealthSummary(getHostHealthSummary(), ApiHealthSummary::getValue);
    }

    private ClusterStatusResult determineClusterStatus(Stack stack) {
        try {
            Collection<ApiService> services = readServices(stack);
            Map<ClusterStatus, List<String>> servicesByStatus = groupServicesByState(services);
            Set<ClusterStatus> statuses = servicesByStatus.keySet();
            if (hasPendingOperation(statuses)) {
                return ClusterStatusResult.of(ClusterStatus.PENDING);
            }
            // service INSTALLED => all its roles are INSTALLED
            if (isUniformStatus(statuses, ClusterStatus.INSTALLED)) {
                return ClusterStatusResult.of(ClusterStatus.INSTALLED);
            }
            // service STARTED => at least one of its roles are STARTED, have to check role statuses
            if (isUniformStatus(statuses, ClusterStatus.STARTED)) {
                return determineClusterStatusFromRoles(stack, servicesByStatus.get(ClusterStatus.STARTED));
            }
            if (areStatesAmbiguous(statuses)) {
                return new ClusterStatusResult(ClusterStatus.AMBIGUOUS, constructStatusMessage(servicesByStatus));
            }

            LOGGER.info("Failed to determine cluster status: {}", statuses);
            return ClusterStatusResult.of(ClusterStatus.UNKNOWN);
        } catch (RuntimeException | ApiException e) {
            LOGGER.info("Failed to determine cluster status: {}", e.getMessage(), e);
            return ClusterStatusResult.of(ClusterStatus.UNKNOWN);
        }
    }

    private ClusterStatusResult determineClusterStatusFromRoles(Stack stack, Collection<String> apiServices) {
        Map<ClusterStatus, List<String>> rolesByStatus = groupRolesByState(stack, apiServices);
        Set<ClusterStatus> statuses = rolesByStatus.keySet();
        if (hasPendingOperation(statuses)) {
            return ClusterStatusResult.of(ClusterStatus.PENDING);
        }
        if (areStatesAmbiguous(statuses)) {
            return new ClusterStatusResult(ClusterStatus.AMBIGUOUS, constructStatusMessage(rolesByStatus));
        }
        if (statuses.size() == 1) {
            return ClusterStatusResult.of(statuses.iterator().next());
        }

        LOGGER.info("Failed to determine cluster status: {}", statuses);
        return ClusterStatusResult.of(ClusterStatus.UNKNOWN);
    }

    private Collection<ApiService> readServices(Stack stack) throws ApiException {
        ServicesResourceApi api = clouderaManagerClientFactory.getServicesResourceApi(client);
        return api.readServices(stack.getCluster().getName(), FULL_VIEW).getItems();
    }

    private static Map<ClusterStatus, List<String>> groupServicesByState(Collection<ApiService> services) {
        return services.stream()
                .filter(service -> !IGNORED_SERVICE_STATES.contains(service.getServiceState()))
                .collect(groupingBy(service -> toClusterStatus(service.getServiceState()),
                        mapping(ApiService::getName, toList())));
    }

    private Map<ClusterStatus, List<String>> groupRolesByState(Stack stack, Collection<String> services) {
        RolesResourceApi api = clouderaManagerClientFactory.getRolesResourceApi(client);
        return services.stream()
                .flatMap(service -> readRoles(api, stack, service))
                .filter(role -> !IGNORED_ROLE_STATES.contains(role.getRoleState()))
                .collect(groupingBy(role -> toClusterStatus(role.getRoleState()),
                        mapping(ApiRole::getName, toList())));
    }

    // need to translate checked exception for usage in stream
    private static Stream<ApiRole> readRoles(RolesResourceApi api, Stack stack, String service) {
        try {
            return api.readRoles(stack.getCluster().getName(), service, "", FULL_VIEW).getItems().stream();
        } catch (ApiException e) {
            String message = String.format("Failed to read roles of service %s %s", stack.getCluster().getName(), service);
            LOGGER.warn(message, e);
            throw new RuntimeException(message, e);
        }
    }

    private boolean isCMRunning() {
        try {
            clouderaManagerClientFactory.getClouderaManagerResourceApi(client).getVersion();
            return true;
        } catch (ApiException e) {
            return false;
        }
    }

    /**
     * Collects summary of HOST_SCM_HEALTH check for each host.
     * Currently this is the best indicator of host availability.
     */
    private Map<String, ApiHealthSummary> getHostHealthSummary() {
        HostsResourceApi api = clouderaManagerClientFactory.getHostsResourceApi(client);
        try {
            return api.readHosts(FULL_VIEW).getItems().stream()
                    .filter(host -> host.getHealthChecks() != null)
                    .flatMap(host -> host.getHealthChecks().stream()
                            .filter(check -> HOST_SCM_HEALTH.equals(check.getName()))
                            .map(ApiHealthCheck::getSummary)
                            .filter(healthSummary -> !IGNORED_HEALTH_SUMMARIES.contains(healthSummary))
                            .map(healthSummary -> Pair.of(host.getHostname(), healthSummary))
                    )
                    .collect(toMap(Pair::getLeft, Pair::getRight));
        } catch (ApiException e) {
            LOGGER.info("Failed to get hosts from CM", e);
            throw new RuntimeException("Failed to get hosts from CM due to: " + e.getMessage(), e);
        }
    }

    private static boolean isUniformStatus(Set<ClusterStatus> statuses, ClusterStatus expectedStatus) {
        return statuses.size() == 1 && statuses.contains(expectedStatus);
    }

    private static boolean areStatesAmbiguous(Set<ClusterStatus> states) {
        return states.containsAll(NON_PENDING_STATES);
    }

    private static String constructStatusMessage(Map<ClusterStatus, List<String>> stateMap) {
        return stateMap.entrySet().stream()
                .map(e -> e.getKey() + ": " + String.join(", ", e.getValue()))
                .collect(joining("; "));
    }

    private static boolean hasPendingOperation(Set<ClusterStatus> states) {
        return states.contains(ClusterStatus.STARTING)
                || states.contains(ClusterStatus.STOPPING);
    }

    private static ClusterStatus toClusterStatus(ApiServiceState state) {
        switch (state) {
            case STARTING:
                return ClusterStatus.STARTING;
            case STARTED:
                return ClusterStatus.STARTED;
            case STOPPING:
                return ClusterStatus.STOPPING;
            case STOPPED:
                return ClusterStatus.INSTALLED;
            case UNKNOWN:
                return ClusterStatus.UNKNOWN;
            default:
                LOGGER.debug("Translated service state {} to status {}", state, ClusterStatus.UNKNOWN);
                return ClusterStatus.UNKNOWN;
        }
    }

    private static ClusterStatus toClusterStatus(ApiRoleState state) {
        switch (state) {
            case STARTING:
                return ClusterStatus.STARTING;
            case STARTED:
                return ClusterStatus.STARTED;
            case STOPPING:
                return ClusterStatus.STOPPING;
            case STOPPED:
                return ClusterStatus.INSTALLED;
            case UNKNOWN:
                return ClusterStatus.UNKNOWN;
            default:
                LOGGER.debug("Translated role state {} to status {}", state, ClusterStatus.UNKNOWN);
                return ClusterStatus.UNKNOWN;
        }
    }

    private static HostMetadataState healthSummaryToState(ApiHealthSummary healthSummary) {
        switch (healthSummary) {
            case GOOD:
            case CONCERNING:
                return HostMetadataState.HEALTHY;
            default:
                LOGGER.debug("Translated health summary {} to state {}", healthSummary, HostMetadataState.UNHEALTHY);
                return HostMetadataState.UNHEALTHY;
        }
    }

    private static <T> Map<String, T> convertHealthSummary(Map<String, ApiHealthSummary> hostHealth, Function<ApiHealthSummary, T> converter) {
        Map<String, T> result = new HashMap<>();
        hostHealth.forEach((hostname, healthSummary) -> result.put(hostname, converter.apply(healthSummary)));
        return result;
    }
}