package com.sequenceiq.cloudbreak.cm;

import static com.cloudera.api.swagger.model.ApiServiceState.STARTED;
import static com.cloudera.api.swagger.model.ApiServiceState.STARTING;
import static com.cloudera.api.swagger.model.ApiServiceState.STOPPED;
import static com.cloudera.api.swagger.model.ApiServiceState.STOPPING;
import static com.sequenceiq.cloudbreak.cm.util.ClouderaManagerConstants.SUMMARY;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.cloudera.api.swagger.ClustersResourceApi;
import com.cloudera.api.swagger.ServicesResourceApi;
import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiCommand;
import com.cloudera.api.swagger.model.ApiCommandList;
import com.cloudera.api.swagger.model.ApiService;
import com.cloudera.api.swagger.model.ApiServiceList;
import com.cloudera.api.swagger.model.ApiServiceState;
import com.sequenceiq.cloudbreak.cm.client.retry.ClouderaManagerApiFactory;
import com.sequenceiq.cloudbreak.cm.exception.ClouderaManagerOperationFailedException;
import com.sequenceiq.cloudbreak.cm.polling.ClouderaManagerPollingServiceProvider;
import com.sequenceiq.cloudbreak.cm.polling.PollingResultErrorHandler;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterCommand;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterCommandType;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;
import com.sequenceiq.cloudbreak.polling.ExtendedPollingResult;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.ClusterCommandService;
import com.sequenceiq.cloudbreak.view.ClusterView;

@Service
public class ClouderaManagerServiceManagementService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClouderaManagerConfigService.class);

    /**
     * Failsafe: max sequential "wait for one active cluster command" cycles before giving up (still-active commands remain).
     * Kept small so we do not mimic an unbounded drain when CM keeps reporting actives.
     */
    private static final int MAX_CLUSTER_ACTIVE_COMMAND_DRAIN_ROUNDS = 10;

    /**
     * Failsafe: wall-clock limit from the start of draining so this phase cannot run for many hours when actives never clear.
     * Checked at each loop iteration before starting another command poll (a single poll may still run up to the CM default timeout).
     */
    private static final Duration MAX_CLUSTER_ACTIVE_COMMAND_DRAIN_DURATION = Duration.ofMinutes(30);

    @Inject
    private ClouderaManagerApiFactory clouderaManagerApiFactory;

    @Inject
    private ClouderaManagerPollingServiceProvider clouderaManagerPollingServiceProvider;

    @Inject
    private PollingResultErrorHandler pollingResultErrorHandler;

    @Inject
    private ClusterCommandService clusterCommandService;

    @Inject
    private ClouderaManagerCommandsService clouderaManagerCommandsService;

    public ApiServiceList readServices(ApiClient client, String clusterName) {
        try {
            LOGGER.debug("Reading services of Cloudera Manager for cluster {}.", clusterName);
            ServicesResourceApi servicesResourceApi = clouderaManagerApiFactory.getServicesResourceApi(client);
            return servicesResourceApi.readServices(clusterName, DataView.SUMMARY.name());
        } catch (ApiException e) {
            LOGGER.error("Failed to get services from Cloudera Manager.", e);
            throw new ClouderaManagerOperationFailedException(e.getMessage(), e);
        }
    }

    public void startClouderaManagerService(ApiClient client, StackDtoDelegate stack, String serviceType, boolean waitForExecution) {
        LOGGER.info("Trying to start services for service type: {} for cluster {}", serviceType, stack.getName());
        ServicesResourceApi servicesResourceApi = clouderaManagerApiFactory.getServicesResourceApi(client);
        String operationName = "start";
        findServiceToPerformOperation(servicesResourceApi, stack, serviceType, Set.of(STARTING, STARTED), operationName)
                .ifPresent(apiService -> {
                    performClouderaManagerOperation(client, stack, operationName, apiService, waitForExecution,
                            servicesResourceApi::startCommand,
                            clouderaManagerPollingServiceProvider::startPollingServiceStart);
                });
    }

    public void stopClouderaManagerService(ApiClient client, StackDtoDelegate stack, String serviceType, boolean waitForExecution) {
        LOGGER.info("Trying to stop services for service type: {} for cluster {}", serviceType, stack.getName());
        ServicesResourceApi servicesResourceApi = clouderaManagerApiFactory.getServicesResourceApi(client);
        String operationName = "stop";
        findServiceToPerformOperation(servicesResourceApi, stack, serviceType, Set.of(STOPPING, STOPPED), operationName)
                .ifPresent(apiService -> {
                    performClouderaManagerOperation(client, stack, operationName, apiService, waitForExecution,
                            servicesResourceApi::stopCommand,
                            clouderaManagerPollingServiceProvider::startPollingServiceStop);
                });
    }

    public void deleteClouderaManagerService(ApiClient client, StackDtoDelegate stack, String serviceType) {
        LOGGER.debug("Trying to delete services for service type: {} for cluster {}", serviceType, stack.getName());
        ServicesResourceApi servicesResourceApi = clouderaManagerApiFactory.getServicesResourceApi(client);
        findServiceOnCluster(stack.getName(), serviceType, servicesResourceApi)
                .ifPresentOrElse(apiService -> {
                    try {
                        LOGGER.debug("Executing delete command on stack {} and service {}", stack.getName(), serviceType);
                        servicesResourceApi.deleteService(stack.getName(), apiService.getName());
                        ExtendedPollingResult result = clouderaManagerPollingServiceProvider.startPollingServiceDeletion(stack, client, serviceType);
                        pollingResultErrorHandler.handlePollingResult(result,
                                String.format("Cluster was terminated while %s service deletion is running.", serviceType),
                                String.format("Timeout happened while %s service deletion is running.", serviceType));
                    } catch (Exception e) {
                        LOGGER.error("Failed to delete services for service type: {} for cluster {}", serviceType, stack.getName(), e);
                        throw new ClouderaManagerOperationFailedException(e.getMessage(), e);
                    }
                }, () -> LOGGER.debug("Unable to delete {} service because is not present on the cluster", serviceType));
    }

    /**
     * Stops all Cloudera Runtime services on the cluster (cluster-level stop command). Waits until no active cluster command is running before
     * submitting the stop, to avoid CM failures when another command is still in progress.
     */
    public void stopAllClusterRuntimeServices(ApiClient client, StackDtoDelegate stack, String clusterName, Collection<ApiService> apiServices)
            throws ApiException, CloudbreakException {
        boolean anyServiceNotStopped = apiServices.stream()
                .anyMatch(service -> !ApiServiceState.STOPPED.equals(service.getServiceState())
                        && !ApiServiceState.STOPPING.equals(service.getServiceState())
                        && !ApiServiceState.NA.equals(service.getServiceState()));
        if (!anyServiceNotStopped) {
            return;
        }
        waitUntilNoActiveClusterCommands(client, stack, clusterName, "stop all cluster services");
        ClustersResourceApi clustersResourceApi = clouderaManagerApiFactory.getClustersResourceApi(client);
        ApiCommand apiCommand = clustersResourceApi.stopCommand(clusterName);
        ExtendedPollingResult pollingResult = clouderaManagerPollingServiceProvider.startPollingCmShutdown(stack, client, apiCommand.getId());
        pollingResultErrorHandler.handlePollingResult(pollingResult, "Cluster was terminated while waiting for Hadoop services to stop",
                "Timeout while stopping Cloudera Manager services.");
    }

    /**
     * Starts all services that are not already started/starting (cluster-level start command). Waits until no active cluster command is running
     * before submitting the start. Tracks the START_CLUSTER command in {@link ClusterCommandService} the same way as the previous implementation.
     */
    public void startAllClusterRuntimeServices(ApiClient client, StackDtoDelegate stack, ClusterView cluster, Collection<ApiService> apiServices)
            throws ApiException, CloudbreakException {
        Set<ApiService> notStartedServices = apiServices.stream()
                .filter(service -> !ApiServiceState.STARTED.equals(service.getServiceState())
                        && !ApiServiceState.STARTING.equals(service.getServiceState())
                        && !ApiServiceState.NA.equals(service.getServiceState()))
                .collect(Collectors.toSet());
        if (notStartedServices.isEmpty()) {
            return;
        }
        LOGGER.debug("Starting cluster because the following services are not running: {}", notStartedServices.stream()
                .map(ApiService::getName)
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet()));
        waitUntilNoActiveClusterCommands(client, stack, stack.getName(), "start all cluster services");
        ClusterCommand startCommand = null;
        try {
            ClustersResourceApi clustersResourceApi = clouderaManagerApiFactory.getClustersResourceApi(client);
            startCommand = startServicesIfNotRunning(cluster, clustersResourceApi, client, stack.getName());
            ExtendedPollingResult pollingResult =
                    clouderaManagerPollingServiceProvider.startPollingCmStartup(stack, client, startCommand.getCommandId());
            pollingResultErrorHandler.handlePollingResult(pollingResult, "Cluster was terminated while waiting for Cloudera Runtime services to start",
                    "Timeout while waiting for Cloudera Runtime services to start");
        } finally {
            if (startCommand != null) {
                clusterCommandService.delete(startCommand);
            }
        }
    }

    private void waitUntilNoActiveClusterCommands(ApiClient client, StackDtoDelegate stack, String clusterName, String beforeOperationLabel)
            throws ApiException, CloudbreakException {
        ClustersResourceApi clustersResourceApi = clouderaManagerApiFactory.getClustersResourceApi(client);
        Instant drainDeadline = Instant.now().plus(MAX_CLUSTER_ACTIVE_COMMAND_DRAIN_DURATION);
        int completedCommandWaits = 0;
        while (true) {
            List<ApiCommand> active = listActiveClusterCommands(clustersResourceApi, clusterName);
            if (active.isEmpty()) {
                return;
            }
            ApiCommand blocking = active.getFirst();
            if (completedCommandWaits >= MAX_CLUSTER_ACTIVE_COMMAND_DRAIN_ROUNDS) {
                throw new ClouderaManagerOperationFailedException(String.format(
                        "Stopped waiting for Cloudera Manager cluster commands after %d sequential waits on cluster %s before %s; "
                                + "still active: %s (id: %s).",
                        MAX_CLUSTER_ACTIVE_COMMAND_DRAIN_ROUNDS, clusterName, beforeOperationLabel, blocking.getName(), blocking.getId()));
            }
            if (Instant.now().isAfter(drainDeadline)) {
                throw new ClouderaManagerOperationFailedException(String.format(
                        "Timed out after %s waiting for active cluster commands on cluster %s before %s; still active: %s (id: %s).",
                        MAX_CLUSTER_ACTIVE_COMMAND_DRAIN_DURATION, clusterName, beforeOperationLabel, blocking.getName(), blocking.getId()));
            }
            LOGGER.debug("Cluster {} has active command {} (id: {}). Waiting until it completes before {}.", clusterName, blocking.getName(),
                    blocking.getId(), beforeOperationLabel);
            ExtendedPollingResult pollingResult = clouderaManagerPollingServiceProvider.startDefaultPolling(stack, client, blocking.getId(),
                    String.format("Active CM cluster command: %s", blocking.getName()));
            pollingResultErrorHandler.handlePollingResult(pollingResult,
                    String.format("Cluster was terminated while waiting for a command before %s.", beforeOperationLabel),
                    String.format("Timeout while waiting for a command before %s.", beforeOperationLabel));
            completedCommandWaits++;
        }
    }

    private List<ApiCommand> listActiveClusterCommands(ClustersResourceApi clustersResourceApi, String clusterName) throws ApiException {
        ApiCommandList apiCommandList = clustersResourceApi.listActiveCommands(clusterName, SUMMARY, null);
        if (apiCommandList.getItems() == null) {
            return List.of();
        }
        return apiCommandList.getItems();
    }

    private ClusterCommand startServicesIfNotRunning(ClusterView cluster, ClustersResourceApi clustersResourceApi, ApiClient apiClient, String stackName)
            throws ApiException {
        Optional<ClusterCommand> startClusterCommand =
                clusterCommandService.findTopByClusterIdAndClusterCommandType(cluster.getId(), ClusterCommandType.START_CLUSTER);
        if (startClusterCommand.isPresent()) {
            Optional<ApiCommand> apiCommand = clouderaManagerCommandsService.getApiCommandIfExist(apiClient, startClusterCommand.get().getCommandId());
            if (apiCommand.isPresent() && Boolean.TRUE.equals(apiCommand.get().isActive())) {
                return startClusterCommand.get();
            } else {
                clusterCommandService.delete(startClusterCommand.get());
                return startServicesAndStoreCMCommand(cluster, clustersResourceApi, stackName);
            }
        } else {
            return startServicesAndStoreCMCommand(cluster, clustersResourceApi, stackName);
        }
    }

    private ClusterCommand startServicesAndStoreCMCommand(ClusterView cluster, ClustersResourceApi clustersResourceApi, String stackName)
            throws ApiException {
        ApiCommand startCommand = clustersResourceApi.startCommand(stackName);
        ClusterCommand newStartClusterCommand = new ClusterCommand();
        newStartClusterCommand.setClusterId(cluster.getId());
        newStartClusterCommand.setCommandId(startCommand.getId());
        newStartClusterCommand.setClusterCommandType(ClusterCommandType.START_CLUSTER);
        return clusterCommandService.save(newStartClusterCommand);
    }

    private Optional<ApiService> findServiceToPerformOperation(ServicesResourceApi servicesResourceApi, StackDtoDelegate stack, String serviceType,
            Set<ApiServiceState> acceptedStates, String operationName) {
        Optional<ApiService> apiService = findServiceOnCluster(stack.getName(), serviceType, servicesResourceApi);
        if (apiService.isPresent() && acceptedStates.stream().noneMatch(acceptedState -> acceptedState.equals(apiService.get().getServiceState()))) {
            return apiService;
        } else {
            LOGGER.debug(apiService.isPresent() ?
                    String.format("Not necessary to %s the service because it is already in %s state.", operationName, apiService.get().getServiceState()) :
                    String.format("Unable to %s %s service because is not present on the cluster", operationName, serviceType));
            return Optional.empty();
        }
    }

    private void performClouderaManagerOperation(ApiClient client, StackDtoDelegate stack, String operationName, ApiService apiService,
            boolean waitForExecution,
            ClouderaManagerServicesCommand clouderaManagerServicesCommand, ClouderaManagerOperationPollerCommand clouderaManagerOperationPollerCommand) {
        try {
            LOGGER.debug("Executing {} command on stack {} and service {}", operationName, stack.getName(), apiService.getType());
            ApiCommand apiCommand = clouderaManagerServicesCommand.apply(stack.getName(), apiService.getName());
            if (waitForExecution) {
                waitForCommandExecution(client, stack, apiCommand, operationName, clouderaManagerOperationPollerCommand);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to {} services for service type: {} for cluster {}", operationName, apiService.getType(), stack.getName(), e);
            throw new ClouderaManagerOperationFailedException(e.getMessage(), e);
        }
    }

    private Optional<ApiService> findServiceOnCluster(String clusterName, String serviceType, ServicesResourceApi servicesResourceApi) {
        try {
            LOGGER.debug("Looking for service of name {} in cluster {}", serviceType, clusterName);
            ApiServiceList serviceList = servicesResourceApi.readServices(clusterName, DataView.SUMMARY.name());
            return serviceList.getItems().stream()
                    .filter(service -> serviceType.equals(service.getType()))
                    .findFirst();
        } catch (ApiException e) {
            String errorMessage = String.format("Failed to get %s service name from Cloudera Manager.", serviceType);
            LOGGER.debug(errorMessage, e);
            throw new ClouderaManagerOperationFailedException(errorMessage, e);
        }
    }

    private void waitForCommandExecution(ApiClient apiClient, StackDtoDelegate stack, ApiCommand apiCommand, String operationName,
            ClouderaManagerOperationPollerCommand clouderaManagerOperationPollerCommand) throws CloudbreakException {
        if (Objects.isNull(apiCommand)) {
            LOGGER.debug("There is no running {} command.", operationName);
        } else {
            LOGGER.debug("Start polling {} command. The command ID is: {}", operationName, apiCommand.getId());
            ExtendedPollingResult pollingResult = clouderaManagerOperationPollerCommand.apply(stack, apiClient, apiCommand.getId());
            pollingResultErrorHandler.handlePollingResult(pollingResult,
                    String.format("Cluster was terminated while %s services command is running.", operationName),
                    String.format("Timeout happened while %s services command is running.", operationName));
        }
    }
}
