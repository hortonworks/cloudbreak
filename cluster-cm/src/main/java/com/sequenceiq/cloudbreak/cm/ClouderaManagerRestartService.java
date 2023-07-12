package com.sequenceiq.cloudbreak.cm;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.cm.DataView.SUMMARY;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_CM_CLUSTER_SERVICES_RESTARTING;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.ClustersResourceApi;
import com.cloudera.api.swagger.ServicesResourceApi;
import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiCommand;
import com.cloudera.api.swagger.model.ApiCommandList;
import com.cloudera.api.swagger.model.ApiRestartClusterArgs;
import com.cloudera.api.swagger.model.ApiRolesToInclude;
import com.cloudera.api.swagger.model.ApiRollingRestartClusterArgs;
import com.cloudera.api.swagger.model.ApiService;
import com.sequenceiq.cloudbreak.cm.client.retry.ClouderaManagerApiFactory;
import com.sequenceiq.cloudbreak.cm.polling.ClouderaManagerPollingServiceProvider;
import com.sequenceiq.cloudbreak.cm.polling.PollingResultErrorHandler;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;
import com.sequenceiq.cloudbreak.polling.ExtendedPollingResult;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;

@Component
public class ClouderaManagerRestartService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClouderaManagerRestartService.class);

    @Inject
    private ClouderaManagerPollingServiceProvider clouderaManagerPollingServiceProvider;

    @Inject
    private PollingResultErrorHandler pollingResultErrorHandler;

    @Inject
    private CloudbreakEventService eventService;

    @Inject
    private ClouderaManagerApiFactory clouderaManagerApiFactory;

    public void doRestartServicesIfNeeded(ApiClient apiClient, StackDtoDelegate stack, boolean rollingRestartEnabled, Optional<List<String>> serviceNames)
            throws ApiException, CloudbreakException {
        LOGGER.debug("Restarting Cloudera Manager services, rollingRestartEnabled: {}", rollingRestartEnabled);
        ClustersResourceApi clustersResourceApi = clouderaManagerApiFactory.getClustersResourceApi(apiClient);
        Optional<ApiCommand> optionalActiveRestartCommand = findActiveRestartCommand(stack, clustersResourceApi, rollingRestartEnabled);
        if (optionalActiveRestartCommand.isPresent()) {
            LOGGER.debug("Restart for Cluster services is already running with id: [{}]", optionalActiveRestartCommand.get().getId());
            waitForRestartExecution(apiClient, stack, optionalActiveRestartCommand.get());
        } else {
            LOGGER.info("Calling restart command. rollingRestartEnabled {}", rollingRestartEnabled);
            ApiCommand restartCommand = rollingRestartEnabled ?
                    executeRollingRestartCommand(apiClient, stack, clustersResourceApi) :
                    executeRestartCommand(stack, clustersResourceApi, serviceNames);
            eventService.fireCloudbreakEvent(stack.getId(), UPDATE_IN_PROGRESS.name(), CLUSTER_CM_CLUSTER_SERVICES_RESTARTING);
            waitForRestartExecution(apiClient, stack, restartCommand);
        }
    }

    public void waitForRestartExecutionIfPresent(ApiClient apiClient, StackDtoDelegate stack, boolean rollingRestartEnabled)
            throws ApiException, CloudbreakException {
        ClustersResourceApi clustersResourceApi = clouderaManagerApiFactory.getClustersResourceApi(apiClient);
        Optional<ApiCommand> optionalRestartCommand = findActiveRestartCommand(stack, clustersResourceApi, rollingRestartEnabled);
        waitForRestartExecution(apiClient, stack, optionalRestartCommand.orElse(null));
    }

    private void waitForRestartExecution(ApiClient apiClient, StackDtoDelegate stack, ApiCommand restartCommand) throws CloudbreakException {
        if (Objects.isNull(restartCommand)) {
            LOGGER.debug("There is no running restart command.");
        } else {
            LOGGER.debug("Start polling restart command. The command ID is: {}", restartCommand.getId());
            ExtendedPollingResult pollingResult = clouderaManagerPollingServiceProvider.startPollingCmServicesRestart(stack, apiClient, restartCommand.getId());
            pollingResultErrorHandler.handlePollingResult(pollingResult, "Cluster was terminated while restarting services.",
                    "Timeout happened while restarting services.");
        }
    }

    private ApiCommand executeRollingRestartCommand(ApiClient apiClient, StackDtoDelegate stack, ClustersResourceApi clustersResourceApi) throws ApiException {
        List<String> serviceNamesToRollingRestart = readServices(stack, apiClient).stream().map(ApiService::getName).collect(Collectors.toList());
        ApiRollingRestartClusterArgs rollingRestartClusterArgs = new ApiRollingRestartClusterArgs();
        rollingRestartClusterArgs.setSlaveBatchSize(BigDecimal.ONE);
        rollingRestartClusterArgs.setRolesToInclude(ApiRolesToInclude.ALL_ROLES);
        rollingRestartClusterArgs.setRestartServiceNames(serviceNamesToRollingRestart);
        return clustersResourceApi.rollingRestart(stack.getName(), rollingRestartClusterArgs);
    }

    private ApiCommand executeRestartCommand(StackDtoDelegate stack, ClustersResourceApi clustersResourceApi, Optional<List<String>> serviceNames)
            throws ApiException {
        ApiRestartClusterArgs restartClusterArgs = new ApiRestartClusterArgs();
        restartClusterArgs.setRedeployClientConfiguration(true);
        serviceNames.ifPresent(restartClusterArgs::setRestartServiceNames);
        return clustersResourceApi.restartCommand(stack.getName(), restartClusterArgs);
    }

    private Optional<ApiCommand> findActiveRestartCommand(StackDtoDelegate stack, ClustersResourceApi clustersResourceApi, boolean rollingRestartEnabled)
            throws ApiException {
        String restartCommandName = determineRestartCommandName(rollingRestartEnabled);
        ApiCommandList apiCommandList = clustersResourceApi.listActiveCommands(stack.getName(), SUMMARY.name(), null);
        return apiCommandList.getItems().stream()
                .filter(cmd -> restartCommandName.equals(cmd.getName())).findFirst();
    }

    private String determineRestartCommandName(boolean rollingRestartEnabled) {
        return rollingRestartEnabled ? "RollingRestart" : "Restart";
    }

    private Collection<ApiService> readServices(StackDtoDelegate stack, ApiClient apiClient) throws ApiException {
        ServicesResourceApi servicesResourceApi = clouderaManagerApiFactory.getServicesResourceApi(apiClient);
        return servicesResourceApi.readServices(stack.getName(), SUMMARY.name()).getItems();
    }
}
