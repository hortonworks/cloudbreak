package com.sequenceiq.cloudbreak.cm;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.cm.DataView.SUMMARY;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_CM_CLUSTER_SERVICES_RESTARTING;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_CM_CLUSTER_SERVICES_ROLLING_RESTART;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.ClustersResourceApi;
import com.cloudera.api.swagger.RoleCommandsResourceApi;
import com.cloudera.api.swagger.RolesResourceApi;
import com.cloudera.api.swagger.ServicesResourceApi;
import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiBulkCommandList;
import com.cloudera.api.swagger.model.ApiCommand;
import com.cloudera.api.swagger.model.ApiCommandList;
import com.cloudera.api.swagger.model.ApiRestartClusterArgs;
import com.cloudera.api.swagger.model.ApiRole;
import com.cloudera.api.swagger.model.ApiRoleList;
import com.cloudera.api.swagger.model.ApiRoleNameList;
import com.cloudera.api.swagger.model.ApiRolesToInclude;
import com.cloudera.api.swagger.model.ApiRollingRestartClusterArgs;
import com.cloudera.api.swagger.model.ApiService;
import com.cloudera.api.swagger.model.ApiServiceList;
import com.sequenceiq.cloudbreak.cm.client.retry.ClouderaManagerApiFactory;
import com.sequenceiq.cloudbreak.cm.exception.ClouderaManagerOperationFailedException;
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

    public void doRestartServicesIfNeeded(ApiClient apiClient, StackDtoDelegate stack, boolean rollingRestartEnabled, boolean restartStaleOnly,
            Optional<List<String>> serviceNames)
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
                    executeRollingRestartCommand(apiClient, stack, restartStaleOnly, clustersResourceApi) :
                    executeRestartCommand(stack, clustersResourceApi, serviceNames);
            eventService.fireCloudbreakEvent(stack.getId(), UPDATE_IN_PROGRESS.name(),
                    rollingRestartEnabled ? CLUSTER_CM_CLUSTER_SERVICES_ROLLING_RESTART : CLUSTER_CM_CLUSTER_SERVICES_RESTARTING);
            waitForRestartExecution(apiClient, stack, restartCommand);
        }
    }

    public void waitForRestartExecutionIfPresent(ApiClient apiClient, StackDtoDelegate stack, boolean rollingRestartEnabled)
            throws ApiException, CloudbreakException {
        ClustersResourceApi clustersResourceApi = clouderaManagerApiFactory.getClustersResourceApi(apiClient);
        Optional<ApiCommand> optionalRestartCommand = findActiveRestartCommand(stack, clustersResourceApi, rollingRestartEnabled);
        waitForRestartExecution(apiClient, stack, optionalRestartCommand.orElse(null));
    }

    public void restartServiceRoleByType(StackDtoDelegate stack, ApiClient apiClient, String serviceType, String roleType) {
        try {
            RolesResourceApi rolesResourceApi = clouderaManagerApiFactory.getRolesResourceApi(apiClient);
            String serviceName = getServiceNameByType(apiClient, stack.getName(), serviceType)
                    .orElseThrow(() -> new ClouderaManagerOperationFailedException(String.format("Cannot find CM service by role '%s' in cluster '%s'.",
                            serviceType, stack.getName())));
            ApiRoleList apiRoleList = rolesResourceApi.readRoles(stack.getName(), serviceName, String.format("type==%s", roleType), SUMMARY.name());
            if (apiRoleList.getItems() == null || apiRoleList.getItems().isEmpty()) {
                throw new ClouderaManagerOperationFailedException(String.format("Cannot find CM service role by type '%s' in cluster '%s'.",
                        roleType, stack.getName()));
            }
            ApiRoleNameList apiRoleNameList = new ApiRoleNameList();
            RoleCommandsResourceApi roleCommandsResourceApi = clouderaManagerApiFactory.getRoleCommandsResourceApi(apiClient);
            for (ApiRole apiRole : apiRoleList.getItems()) {
                apiRoleNameList.addItemsItem(apiRole.getName());
            }
            ApiBulkCommandList apiCommands = roleCommandsResourceApi.restartCommand(stack.getName(), serviceName, apiRoleNameList);
            for (ApiCommand apiCommand : apiCommands.getItems()) {
                waitForRestartExecution(apiClient, stack, apiCommand);
            }
        } catch (ApiException | CloudbreakException e) {
            LOGGER.info("Could not restart services", e);
            throw new ClouderaManagerOperationFailedException(e.getMessage(), e);
        }
    }

    private Optional<String> getServiceNameByType(ApiClient apiClient, String clusterName, String serviceType) throws ApiException {
        ServicesResourceApi servicesResourceApi = clouderaManagerApiFactory.getServicesResourceApi(apiClient);
        ApiServiceList apiServiceList = servicesResourceApi.readServices(clusterName, SUMMARY.name());
        return apiServiceList.getItems().stream()
                .filter(apiService -> serviceType.equals(apiService.getType()))
                .map(ApiService::getName)
                .findFirst();
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

    private ApiCommand executeRollingRestartCommand(ApiClient apiClient, StackDtoDelegate stack, boolean restartStaleOnly,
            ClustersResourceApi clustersResourceApi) throws ApiException {
        List<String> serviceNamesToRollingRestart = readServices(stack, apiClient).stream().map(ApiService::getName).collect(Collectors.toList());
        ApiRollingRestartClusterArgs rollingRestartClusterArgs = new ApiRollingRestartClusterArgs();
        rollingRestartClusterArgs.setSlaveBatchSize(BigDecimal.ONE);
        rollingRestartClusterArgs.setRolesToInclude(ApiRolesToInclude.ALL_ROLES);
        rollingRestartClusterArgs.setRestartServiceNames(serviceNamesToRollingRestart);
        rollingRestartClusterArgs.setStaleConfigsOnly(restartStaleOnly);

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
