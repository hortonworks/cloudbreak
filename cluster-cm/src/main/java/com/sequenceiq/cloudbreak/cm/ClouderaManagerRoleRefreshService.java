package com.sequenceiq.cloudbreak.cm;

import static com.sequenceiq.cloudbreak.polling.PollingResult.isExited;
import static com.sequenceiq.cloudbreak.polling.PollingResult.isTimeout;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.cloudera.api.swagger.RoleCommandsResourceApi;
import com.cloudera.api.swagger.RolesResourceApi;
import com.cloudera.api.swagger.ServicesResourceApi;
import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiBulkCommandList;
import com.cloudera.api.swagger.model.ApiCommand;
import com.cloudera.api.swagger.model.ApiRole;
import com.cloudera.api.swagger.model.ApiRoleList;
import com.cloudera.api.swagger.model.ApiRoleNameList;
import com.cloudera.api.swagger.model.ApiService;
import com.sequenceiq.cloudbreak.cloud.scheduler.CancellationException;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerClientFactory;
import com.sequenceiq.cloudbreak.cm.polling.ClouderaManagerPollingServiceProvider;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.polling.PollingResult;
import com.sequenceiq.cloudbreak.service.CloudbreakException;

@Service
class ClouderaManagerRoleRefreshService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClouderaManagerRoleRefreshService.class);

    private static final String SUMMARY = "SUMMARY";

    private static final String FILTER = null;

    private static final String VIEW = null;

    @Inject
    private ClouderaManagerClientFactory clouderaManagerClientFactory;

    @Inject
    private ClouderaManagerPollingServiceProvider clouderaManagerPollingServiceProvider;

    void refreshClusterRoles(ApiClient client, Stack stack) throws ApiException, CloudbreakException {
        LOGGER.debug("Cluster role refresh has been started.");
        RoleCommandsResourceApi roleCommandsResourceApi = clouderaManagerClientFactory.getRoleCommandsResourceApi(client);
        RolesResourceApi rolesResourceApi = clouderaManagerClientFactory.getRolesResourceApi(client);
        ServicesResourceApi servicesResourceApi = clouderaManagerClientFactory.getServicesResourceApi(client);

        List<ApiService> services = servicesResourceApi.readServices(stack.getName(), SUMMARY).getItems();

        for (ApiService service : services) {
            LOGGER.debug(String.format("Refreshing %s roles.", service.getName()));
            ApiRoleList apiRoleList = rolesResourceApi.readRoles(stack.getName(), service.getName(), FILTER, VIEW);
            ApiRoleNameList apiRoleNameList = getRoleNameList(apiRoleList);
            ApiBulkCommandList refreshCommand = roleCommandsResourceApi.refreshCommand(stack.getName(), service.getName(), apiRoleNameList);
            restartRolesByService(refreshCommand.getItems(), client, stack);
        }
        LOGGER.debug("Cluster role refresh finished successfully.");
    }

    private ApiRoleNameList getRoleNameList(ApiRoleList apiRoleList) {
        ApiRoleNameList apiRoleNameList = new ApiRoleNameList();
        apiRoleNameList.setItems(apiRoleList.getItems().stream().map(ApiRole::getName).collect(Collectors.toList()));
        return apiRoleNameList;
    }

    private void restartRolesByService(List<ApiCommand> commands, ApiClient client, Stack stack) throws CloudbreakException {
        for (ApiCommand command : commands) {
            PollingResult pollingResult = clouderaManagerPollingServiceProvider.refreshClusterPollingService(stack, client, command.getId());
            if (isExited(pollingResult)) {
                throw new CancellationException("Cluster was terminated while waiting for cluster refresh");
            } else if (isTimeout(pollingResult)) {
                throw new CloudbreakException("Timeout while Cloudera Manager tried to refresh the cluster..");
            }
        }
    }
}
