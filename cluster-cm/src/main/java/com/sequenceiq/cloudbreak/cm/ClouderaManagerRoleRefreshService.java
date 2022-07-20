package com.sequenceiq.cloudbreak.cm;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.cloudera.api.swagger.ClouderaManagerResourceApi;
import com.cloudera.api.swagger.ClustersResourceApi;
import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiCommand;
import com.cloudera.api.swagger.model.ApiCommandList;
import com.sequenceiq.cloudbreak.cloud.scheduler.CancellationException;
import com.sequenceiq.cloudbreak.cm.client.retry.ClouderaManagerApiFactory;
import com.sequenceiq.cloudbreak.cm.polling.ClouderaManagerPollingServiceProvider;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;
import com.sequenceiq.cloudbreak.polling.ExtendedPollingResult;
import com.sequenceiq.cloudbreak.service.CloudbreakException;

@Service
class ClouderaManagerRoleRefreshService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClouderaManagerRoleRefreshService.class);

    private static final String GENERATE_CREDENTIALS_COMMAND_NAME = "GenerateCredentials";

    @Inject
    private ClouderaManagerApiFactory clouderaManagerApiFactory;

    @Inject
    private ClouderaManagerPollingServiceProvider clouderaManagerPollingServiceProvider;

    void refreshClusterRoles(ApiClient client, StackDtoDelegate stack) throws ApiException, CloudbreakException {
        waitForGenerateCredentialsToFinish(stack, client);
        ClustersResourceApi clustersResourceApi = clouderaManagerApiFactory.getClustersResourceApi(client);
        ApiCommand refreshCommand = clustersResourceApi.refresh(stack.getCluster().getName());
        LOGGER.debug("Cluster role refresh has been started.");
        pollingRefresh(refreshCommand, client, stack);
        LOGGER.debug("Cluster role refresh finished successfully.");
    }

    private void pollingRefresh(ApiCommand command, ApiClient client, StackDtoDelegate stack) throws CloudbreakException {
        ExtendedPollingResult pollingResult = new ExtendedPollingResult.ExtendedPollingResultBuilder().success().build();
        try {
            pollingResult = clouderaManagerPollingServiceProvider.startPollingCmConfigurationRefresh(stack, client, command.getId());
        } catch (ClouderaManagerOperationFailedException e) {
            LOGGER.warn("Ignored failed refresh command. Upscale will continue.", e);
        }
        if (pollingResult.isExited()) {
            throw new CancellationException("Cluster was terminated while waiting for cluster refresh");
        } else if (pollingResult.isTimeout()) {
            throw new CloudbreakException("Timeout while Cloudera Manager tried to refresh the cluster..");
        }
    }

    private void waitForGenerateCredentialsToFinish(StackDtoDelegate stack, ApiClient client) throws ApiException {
        LOGGER.debug("Wait if Generate Credentials command is still active.");
        ClouderaManagerResourceApi clouderaManagerResourceApi = clouderaManagerApiFactory.getClouderaManagerResourceApi(client);
        ApiCommandList apiCommandList = clouderaManagerResourceApi.listActiveCommands(DataView.SUMMARY.name());
        Optional<BigDecimal> generateCredentialsCommandId = apiCommandList.getItems().stream()
                .filter(toGenerateCredentialsCommand()).map(ApiCommand::getId).findFirst();
        generateCredentialsCommandId.ifPresent(pollCredentialGeneration(stack, client));
    }

    private Predicate<ApiCommand> toGenerateCredentialsCommand() {
        return apiCommand -> GENERATE_CREDENTIALS_COMMAND_NAME.equals(apiCommand.getName());
    }

    private Consumer<BigDecimal> pollCredentialGeneration(StackDtoDelegate stack, ApiClient client) {
        return id -> {
            LOGGER.debug("Generate Credentials command is still active.");
            clouderaManagerPollingServiceProvider.startPollingCmGenerateCredentials(stack, client, id);
        };
    }
}
