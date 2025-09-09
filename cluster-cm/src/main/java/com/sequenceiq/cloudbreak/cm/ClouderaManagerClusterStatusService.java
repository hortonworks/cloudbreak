package com.sequenceiq.cloudbreak.cm;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
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
import com.cloudera.api.swagger.model.ApiClusterTemplate;
import com.cloudera.api.swagger.model.ApiCommand;
import com.cloudera.api.swagger.model.ApiHost;
import com.cloudera.api.swagger.model.ApiHostList;
import com.cloudera.api.swagger.model.ApiRole;
import com.cloudera.api.swagger.model.ApiRoleState;
import com.cloudera.api.swagger.model.ApiService;
import com.cloudera.api.swagger.model.ApiServiceList;
import com.cloudera.api.swagger.model.ApiServiceState;
import com.cloudera.api.swagger.model.ApiVersionInfo;
import com.fasterxml.jackson.databind.JsonNode;
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
import com.sequenceiq.cloudbreak.cm.converter.ApiClusterTemplateToCmTemplateConverter;
import com.sequenceiq.cloudbreak.cm.exception.ClouderaManagerOperationFailedException;
import com.sequenceiq.cloudbreak.cm.polling.ClouderaManagerPollingServiceProvider;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.common.metrics.MetricService;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterCommandType;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.view.ClusterView;

@Service
@Scope("prototype")
public class ClouderaManagerClusterStatusService implements ClusterStatusService {

    static final String FULL_VIEW = "FULL";

    static final String SUMMARY = "summary";

    private static final String UNKNOWN = "UNKNOWN";

    private static final String CODE = "code";

    private static final String STACK_SYNC_CM_UNREACHABLE = "stack_sync_cm_unreachable";

    private static final String STATUS_CODE = "STATUS_CODE";

    private static final String ERROR_CODE = "ERROR_CODE";

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

    private final StackDtoDelegate stack;

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

    @Inject
    private ClouderaManagerHealthService clouderaManagerHealthService;

    @Inject
    private ClouderaManagerPollingServiceProvider clouderaManagerPollingServiceProvider;

    @Inject
    private ApiClusterTemplateToCmTemplateConverter apiClusterTemplateToCmTemplateConverter;

    @Qualifier("CommonMetricService")
    @Inject
    private MetricService metricService;

    private ApiClient client;

    private ApiClient fastClient;

    private ApiClient v52Client;

    ClouderaManagerClusterStatusService(StackDtoDelegate stack, HttpClientConfig clientConfig) {
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
    private static Stream<ApiRole> readRoles(RolesResourceApi api, StackDtoDelegate stack, String service) {
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

    @PostConstruct
    public void initApiClient() throws ClusterClientInitException {
        ClusterView cluster = stack.getCluster();
        String cloudbreakClusterManagerUser = cluster.getCloudbreakClusterManagerUser();
        String cloudbreakClusterManagerPassword = cluster.getCloudbreakClusterManagerPassword();
        try {
            client = clouderaManagerApiClientProvider
                    .getV31Client(stack.getGatewayPort(), cloudbreakClusterManagerUser, cloudbreakClusterManagerPassword, clientConfig);
            fastClient = clouderaManagerApiClientProvider
                    .getV31Client(stack.getGatewayPort(), cloudbreakClusterManagerUser, cloudbreakClusterManagerPassword, clientConfig);
            fastClient.setHttpClient(fastClient.getHttpClient().newBuilder().connectTimeout(connectQuickTimeoutSeconds, TimeUnit.SECONDS).build());
            v52Client = clouderaManagerApiClientProvider
                    .getV52Client(stack.getGatewayPort(), cloudbreakClusterManagerUser, cloudbreakClusterManagerPassword, clientConfig);
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
        return clouderaManagerHealthService.getExtendedHostStatuses(client, runtimeVersion);
    }

    @Override
    public List<String> getDecommissionedHostsFromCM() {
        HostsResourceApi api = clouderaManagerApiFactory.getHostsResourceApi(client);
        try {
            ApiHostList apiHostList = api.readHosts(null, null, SUMMARY);
            LOGGER.trace("Response from CM for readHosts call: {}", apiHostList);
            return apiHostList.getItems()
                    .stream()
                    .filter(host -> Boolean.TRUE.equals(host.isMaintenanceMode()))
                    .map(ApiHost::getHostname)
                    .collect(toList());
        } catch (ApiException e) {
            LOGGER.info("Failed to get hosts from CM", e);
            throw new RuntimeException("Failed to get hosts from CM due to: " + e.getMessage(), e);
        }
    }

    @Override
    public Map<HostName, String> getHostStatusesRaw() {
        return clouderaManagerHealthService.getHostStatusesRaw(client);
    }

    private ClusterStatusResult determineClusterStatus(StackDtoDelegate stack) {
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

    private ClusterStatusResult determineClusterStatusFromRoles(StackDtoDelegate stack, Collection<String> apiServices) {
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

    private Collection<ApiService> readServices(StackDtoDelegate stack) throws ApiException {
        ServicesResourceApi api = clouderaManagerApiFactory.getServicesResourceApi(client);
        return api.readServices(stack.getCluster().getName(), FULL_VIEW).getItems();
    }

    private Map<ClusterStatus, List<String>> groupRolesByState(StackDtoDelegate stack, Collection<String> services) {
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
            metricService.incrementMetricCounter(STACK_SYNC_CM_UNREACHABLE,
                    STATUS_CODE, String.valueOf(e.getCode()),
                    ERROR_CODE, extractErrorCode(e));
            return false;
        }
    }

    @Override
    public boolean isServiceRunningByType(String clusterName, String serviceType) {
        try {
            ServicesResourceApi servicesResourceApi = clouderaManagerApiFactory.getServicesResourceApi(client);
            ApiServiceList apiServiceList = servicesResourceApi.readServices(clusterName, SUMMARY);
            return apiServiceList.getItems().stream()
                    .filter(apiService -> serviceType.equals(apiService.getType()))
                    .filter(apiService -> apiService.getServiceState() == ApiServiceState.STARTED)
                    .findFirst()
                    .isPresent();
        } catch (ApiException e) {
            LOGGER.info(String.format("Failed to get the service state from CM for service type '%s' in cluster '%s'", serviceType, clusterName), e);
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
    public List<ClusterManagerCommand> getActiveCommandsList() {
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
    public Optional<ClusterManagerCommand> findCommand(StackDtoDelegate stack, ClusterCommandType command) {
        try {
            ClustersResourceApi clustersResourceApi = clouderaManagerApiFactory.getClustersResourceApi(client);
            ClouderaManagerCommand clouderaManagerCommand = ClouderaManagerCommand.ofType(command);
            if (clouderaManagerCommand == null) {
                return Optional.empty();
            }
            Optional<BigDecimal> commandId = syncApiCommandRetriever.getCommandId(clouderaManagerCommand.getName(), clustersResourceApi, stack.getStack());
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

    @Override
    public boolean waitForHealthyServices(Optional<String> runtimeVersion) {
        try {
            return clouderaManagerPollingServiceProvider
                    .startPollingCmHostServicesHealthy(stack, client, clouderaManagerHealthService, runtimeVersion)
                    .isSuccess();
        } catch (Exception e) {
            LOGGER.warn("Unexpected error while waiting for services to be in a healthy status.", e);
        }
        return false;
    }

    @Override
    public String getDeployment(Stack stack) {
        String clusterName = stack.getName();
        String extendedBlueprintText = stack.getCluster().getExtendedBlueprintText();
        try {
            ApiClusterTemplate apiClusterTemplate = clouderaManagerApiFactory
                    .getClustersResourceApi(v52Client)
                    .export(clusterName, Boolean.FALSE);
            return apiClusterTemplateToCmTemplateConverter.convert(apiClusterTemplate, extendedBlueprintText);
        } catch (ApiException e) {
            LOGGER.warn("Failed to get deployment from CM", e);
            throw new ClouderaManagerOperationFailedException("Failed to get deployment from CM", e);
        }
    }

    private ClusterManagerCommand convertApiCommand(ApiCommand apiCommand) {
        if (Objects.isNull(apiCommand)) {
            return null;
        }
        ClusterManagerCommand command = new ClusterManagerCommand();
        command.setId(apiCommand.getId());
        command.setName(apiCommand.getName());
        command.setSuccess(apiCommand.isSuccess());
        command.setActive(apiCommand.isActive());
        command.setRetryable(apiCommand.isCanRetry());
        command.setEndTime(apiCommand.getEndTime());
        command.setResultMessage(apiCommand.getResultMessage());
        command.setStartTime(apiCommand.getStartTime());
        return command;
    }

    private List<ClusterManagerCommand> convertCommandsList(List<ApiCommand> activeCommands) {
        if (activeCommands == null) {
            return List.of();
        } else {
            return activeCommands.stream().map(this::convertApiCommand).collect(toList());
        }
    }

    private String extractErrorCode(ApiException apiException) {
        if (StringUtils.isNotEmpty(apiException.getResponseBody())) {
            try {
                JsonNode tree = JsonUtil.readTree(apiException.getResponseBody());
                JsonNode code = tree.get(CODE);
                if (code != null && code.isTextual()) {
                    String text = code.asText();
                    if (StringUtils.isNotEmpty(text)) {
                        return text;
                    }
                }
            } catch (Exception e) {
                LOGGER.debug("Failed to parse API response body as JSON", e);
            }
        }
        return UNKNOWN;
    }
}
