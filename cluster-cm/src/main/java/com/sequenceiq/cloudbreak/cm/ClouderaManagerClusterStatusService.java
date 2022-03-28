package com.sequenceiq.cloudbreak.cm;

import static com.sequenceiq.cloudbreak.cloud.model.HostName.hostName;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.collections4.CollectionUtils.emptyIfNull;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;

import com.cloudera.api.swagger.ClouderaManagerResourceApi;
import com.cloudera.api.swagger.ClustersResourceApi;
import com.cloudera.api.swagger.HostsResourceApi;
import com.cloudera.api.swagger.RolesResourceApi;
import com.cloudera.api.swagger.ServicesResourceApi;
import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiCommand;
import com.cloudera.api.swagger.model.ApiHealthCheck;
import com.cloudera.api.swagger.model.ApiHealthSummary;
import com.cloudera.api.swagger.model.ApiHost;
import com.cloudera.api.swagger.model.ApiHostList;
import com.cloudera.api.swagger.model.ApiRole;
import com.cloudera.api.swagger.model.ApiRoleRef;
import com.cloudera.api.swagger.model.ApiRoleState;
import com.cloudera.api.swagger.model.ApiService;
import com.cloudera.api.swagger.model.ApiServiceState;
import com.cloudera.api.swagger.model.ApiVersionInfo;
import com.google.common.base.Joiner;
import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.client.HttpClientConfig;
import com.sequenceiq.cloudbreak.cloud.model.HostName;
import com.sequenceiq.cloudbreak.cluster.api.ClusterStatusService;
import com.sequenceiq.cloudbreak.cluster.model.ClusterManagerCommand;
import com.sequenceiq.cloudbreak.cluster.service.ClusterClientInitException;
import com.sequenceiq.cloudbreak.cluster.status.ClusterStatus;
import com.sequenceiq.cloudbreak.cluster.status.ClusterStatusResult;
import com.sequenceiq.cloudbreak.cluster.status.ExtendedHostStatuses;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerApiClientProvider;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerClientInitException;
import com.sequenceiq.cloudbreak.cm.client.retry.ClouderaManagerApiFactory;
import com.sequenceiq.cloudbreak.cm.commands.SyncApiCommandRetriever;
import com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil;
import com.sequenceiq.cloudbreak.common.type.HealthCheck;
import com.sequenceiq.cloudbreak.common.type.HealthCheckResult;
import com.sequenceiq.cloudbreak.common.type.HealthCheckType;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterCommandType;
import com.sequenceiq.cloudbreak.service.CloudbreakException;

@Service
@Scope("prototype")
public class ClouderaManagerClusterStatusService implements ClusterStatusService {

    static final String HOST_SCM_HEALTH = "HOST_SCM_HEALTH";

    static final String HOST_AGENT_CERTIFICATE_EXPIRY = "HOST_AGENT_CERTIFICATE_EXPIRY";

    static final String FULL_VIEW = "FULL";

    static final String FULL_WITH_EXPLANATION_VIEW = "FULL_WITH_HEALTH_CHECK_EXPLANATION";

    static final String SUMMARY = "summary";

    private static final String DEFAULT_STATUS_REASON = "Cloudera Manager reported bad health for this host.";

    private static final String MAINTENANCE_MODE = "This host is in maintenance mode.";

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

    private static final Set<Function<ApiHost, Optional<HealthCheck>>> HEALTH_CHECK_FUNCTIONS = Sets.newHashSet(
            ClouderaManagerClusterStatusService::getHostHealthCheck,
            ClouderaManagerClusterStatusService::getServicesHealthCheck,
            ClouderaManagerClusterStatusService::getCertCheck
    );

    private final Stack stack;

    private final HttpClientConfig clientConfig;

    @Value("${cb.cm.client.connect.quicktimeout.seconds:15}")
    private Integer connectQuickTimeoutSeconds;

    @Inject
    private ClouderaManagerApiClientProvider clouderaManagerApiClientProvider;

    @Inject
    private ClouderaManagerApiFactory clouderaManagerApiFactory;

    @Inject
    private RetryTemplate cmApiRetryTemplate;

    @Inject
    private SyncApiCommandRetriever syncApiCommandRetriever;

    @Inject
    private ClouderaManagerCommandsService clouderaManagerCommandsService;

    private ApiClient client;

    private ApiClient fastClient;

    ClouderaManagerClusterStatusService(Stack stack, HttpClientConfig clientConfig) {
        this.stack = stack;
        this.clientConfig = clientConfig;
    }

    private static Map<ClusterStatus, List<String>> groupServicesByState(Collection<ApiService> services) {
        return services.stream()
                .filter(service -> !IGNORED_SERVICE_STATES.contains(service.getServiceState()))
                .collect(groupingBy(service -> toClusterStatus(service.getServiceState()),
                        mapping(ApiService::getName, toList())));
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

    @PostConstruct
    public void initApiClient() throws ClusterClientInitException {
        Cluster cluster = stack.getCluster();
        String cloudbreakAmbariUser = cluster.getCloudbreakAmbariUser();
        String cloudbreakAmbariPassword = cluster.getCloudbreakAmbariPassword();
        try {
            client = clouderaManagerApiClientProvider
                    .getV31Client(stack.getGatewayPort(), cloudbreakAmbariUser, cloudbreakAmbariPassword, clientConfig);
            fastClient = clouderaManagerApiClientProvider
                    .getV31Client(stack.getGatewayPort(), cloudbreakAmbariUser, cloudbreakAmbariPassword, clientConfig);
            fastClient.getHttpClient().setConnectTimeout(connectQuickTimeoutSeconds, TimeUnit.SECONDS);
        } catch (ClouderaManagerClientInitException e) {
            throw new ClusterClientInitException(e);
        }
    }

    @Override
    public ClusterStatusResult getStatus(boolean blueprintPresent) {
        if (!isClusterManagerRunning()) {
            return ClusterStatusResult.of(ClusterStatus.CLUSTERMANAGER_NOT_RUNNING);
        } else if (blueprintPresent) {
            return determineClusterStatus(stack);
        } else {
            return ClusterStatusResult.of(ClusterStatus.CLUSTERMANAGER_RUNNING);
        }
    }

    @Override
    public ExtendedHostStatuses getExtendedHostStatuses(Optional<String> runtimeVersion) {
        List<ApiHost> apiHostList = getHostsFromCM();
        boolean cmServicesHealthCheckAllowed = CMRepositoryVersionUtil.isCmServicesHealthCheckAllowed(runtimeVersion);
        Map<HostName, Set<HealthCheck>> hostStates = apiHostList.stream().collect(Collectors.toMap(
                apiHost -> hostName(apiHost.getHostname()), apiHost -> getHealthChecks(apiHost, cmServicesHealthCheckAllowed)));
        hostStates.entrySet().removeIf(entry -> entry.getValue().isEmpty());
        LOGGER.debug("Creating 'ExtendedHostStatuses' with {}", hostStates);
        return new ExtendedHostStatuses(hostStates);
    }

    @Override
    public List<String> getDecommissionedHostsFromCM() {
        HostsResourceApi api = clouderaManagerApiFactory.getHostsResourceApi(client);
        try {
            ApiHostList apiHostList = api.readHosts(null, null, SUMMARY);
            LOGGER.trace("Response from CM for readHosts call: {}", apiHostList);
            return apiHostList.getItems()
                    .stream()
                    .filter(host -> Boolean.TRUE.equals(host.getMaintenanceMode()))
                    .map(ApiHost::getHostname)
                    .collect(toList());
        } catch (ApiException e) {
            LOGGER.info("Failed to get hosts from CM", e);
            throw new RuntimeException("Failed to get hosts from CM due to: " + e.getMessage(), e);
        }
    }

    private static Set<HealthCheck> getHealthChecks(ApiHost apiHost, boolean cmServicesHealthCheckAllowed) {
        Set<HealthCheck> healthChecks = HEALTH_CHECK_FUNCTIONS.stream()
                .map(healthCheck -> healthCheck.apply(apiHost))
                .flatMap(Optional::stream)
                .collect(Collectors.toSet());
        healthChecks.removeIf(healthCheck -> HealthCheckType.SERVICES.equals(healthCheck.getType()) && !cmServicesHealthCheckAllowed);
        return healthChecks;
    }

    private static Optional<HealthCheck> getServicesHealthCheck(ApiHost host) {
        Set<String> servicesWithBadHealth = collectServicesWithBadHealthOnHost(host);
        if (!servicesWithBadHealth.isEmpty()) {
            String statusReason = String.format("The following services are in bad health: %s.", Joiner.on(", ").join(servicesWithBadHealth));
            return Optional.of(new HealthCheck(HealthCheckType.SERVICES, HealthCheckResult.UNHEALTHY, Optional.of(statusReason)));
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
            return Optional.of(new HealthCheck(HealthCheckType.CERT, result, reason));
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
                        getHostHealthMessage(apiHealthCheck.getSummary(), apiHealthCheck.getExplanation(), apiHost.getMaintenanceMode())));
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

    @Override
    public Map<HostName, String> getHostStatusesRaw() {
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
        ServicesResourceApi api = clouderaManagerApiFactory.getServicesResourceApi(client);
        return api.readServices(stack.getCluster().getName(), FULL_VIEW).getItems();
    }

    private Map<ClusterStatus, List<String>> groupRolesByState(Stack stack, Collection<String> services) {
        RolesResourceApi api = clouderaManagerApiFactory.getRolesResourceApi(client);
        return services.stream()
                .flatMap(service -> readRoles(api, stack, service))
                .filter(role -> !IGNORED_ROLE_STATES.contains(role.getRoleState()))
                .collect(groupingBy(role -> toClusterStatus(Optional.ofNullable(role.getRoleState()).orElse(ApiRoleState.UNKNOWN)),
                        mapping(ApiRole::getName, toList())));
    }

    @Override
    public boolean isClusterManagerRunning() {
        try {
            cmApiRetryTemplate.execute(context -> clouderaManagerApiFactory.getClouderaManagerResourceApi(client).getVersion());
            return true;
        } catch (ApiException e) {
            LOGGER.info("Failed to get version from CM", e);
            return false;
        }
    }

    @Override
    public boolean isClusterManagerRunningQuickCheck() {
        try {
            clouderaManagerApiFactory.getClouderaManagerResourceApi(fastClient).getVersion();
            return true;
        } catch (ApiException e) {
            LOGGER.info("Failed to get version from CM", e);
            return false;
        }
    }

    @Override
    public Optional<String> getClusterManagerVersion() {
        try {
            ApiVersionInfo apiVersionInfo = cmApiRetryTemplate.execute(context -> clouderaManagerApiFactory.getClouderaManagerResourceApi(client).getVersion());
            return Optional.ofNullable(apiVersionInfo.getVersion());
        } catch (ApiException e) {
            LOGGER.info("Failed to get version from CM: ", e);
            throw new ClouderaManagerOperationFailedException("Failed to get CM version from CM", e);
        }
    }

    @Override
    public List<String> getActiveCommandsList() {
        try {
            ClouderaManagerResourceApi clouderaManagerResourceApi = clouderaManagerApiFactory.getClouderaManagerResourceApi(client);
            List<ApiCommand> activeCommands = clouderaManagerResourceApi.listActiveCommands("SUMMARY").getItems();
            LOGGER.debug("Cloudera Manager active commands: {}", activeCommands);
            return convertCommandsList(activeCommands);
        } catch (ApiException e) {
            LOGGER.info("Failed to get active commands from CM: ", e);
            throw new ClouderaManagerOperationFailedException("Failed to get active commands from CM", e);
        }
    }

    @Override
    public Optional<ClusterManagerCommand> findCommand(Stack stack, ClusterCommandType command) {
        try {
            ClustersResourceApi clustersResourceApi = clouderaManagerApiFactory.getClustersResourceApi(client);
            ClouderaManagerCommand clouderaManagerCommand = ClouderaManagerCommand.ofType(command);
            if (clouderaManagerCommand == null) {
                return Optional.empty();
            }
            Optional<BigDecimal> commandId = syncApiCommandRetriever.getCommandId(clouderaManagerCommand.getName(), clustersResourceApi, stack);
            if (commandId.isPresent()) {
                return Optional.ofNullable(convertApiCommand(clouderaManagerCommandsService.getApiCommand(client, commandId.get())));
            } else {
                LOGGER.info("Command {} could not been found in CM for stack {}", command, stack.getName());
                return Optional.empty();
            }
        } catch (CloudbreakException | ApiException e) {
            LOGGER.warn("Unexpected error during CM command table fetching, assuming no such command exists", e);
            return Optional.empty();
        }
    }

    private ClusterManagerCommand convertApiCommand(ApiCommand apiCommand) {
        if (Objects.isNull(apiCommand)) {
            return null;
        }
        ClusterManagerCommand command = new ClusterManagerCommand();
        command.setId(apiCommand.getId());
        command.setName(apiCommand.getName());
        command.setSuccess(apiCommand.getSuccess());
        command.setActive(apiCommand.getActive());
        command.setRetryable(apiCommand.getCanRetry());
        command.setEndTime(apiCommand.getEndTime());
        command.setResultMessage(apiCommand.getResultMessage());
        command.setStartTime(apiCommand.getStartTime());
        return command;
    }

    private List<String> convertCommandsList(List<ApiCommand> activeCommands) {
        if (activeCommands == null) {
            return List.of();
        } else {
            return activeCommands.stream()
                    .map(cmd -> "ApiCommand[id: " + cmd.getId() + ", name: " + cmd.getName() + ", starttime: " + cmd.getStartTime() + "]")
                    .collect(Collectors.toList());
        }
    }

    /**
     * Collects summary of HOST_SCM_HEALTH check for each host.
     * Currently this is the best indicator of host availability.
     */
    private Map<HostName, ApiHealthSummary> getHostHealthSummary() {
        HostsResourceApi api = clouderaManagerApiFactory.getHostsResourceApi(client);
        try {
            return api.readHosts(null, null, FULL_VIEW).getItems().stream()
                    .filter(host -> host.getHealthChecks() != null)
                    .flatMap(host -> host.getHealthChecks().stream()
                            .filter(check -> HOST_SCM_HEALTH.equals(check.getName()))
                            .map(ApiHealthCheck::getSummary)
                            .filter(healthSummary -> !IGNORED_HEALTH_SUMMARIES.contains(healthSummary))
                            .map(healthSummary -> Pair.of(hostName(host.getHostname()), healthSummary))
                    )
                    .collect(toMap(Pair::getLeft, Pair::getRight));
        } catch (ApiException e) {
            LOGGER.info("Failed to get hosts from CM", e);
            throw new RuntimeException("Failed to get hosts from CM due to: " + e.getMessage(), e);
        }
    }

    private List<ApiHost> getHostsFromCM() {
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
}