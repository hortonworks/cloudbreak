package com.sequenceiq.cloudbreak.cloud.azure.client;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.azure.resourcemanager.postgresql.PostgreSqlManager;
import com.azure.resourcemanager.postgresql.models.Server;
import com.azure.resourcemanager.postgresql.models.ServerState;
import com.sequenceiq.cloudbreak.cloud.azure.util.AzureExceptionHandler;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;

public class AzureSingleServerClient extends AbstractAzureServiceClient {

    public static final ServerState UNKNOWN = ServerState.fromString("UNKNOWN");

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureSingleServerClient.class);

    private final PostgreSqlManager postgreSqlManager;

    public AzureSingleServerClient(PostgreSqlManager postgreSqlManager, AzureExceptionHandler azureExceptionHandler,
            AzureListResultFactory azureListResultFactory) {
        super(azureExceptionHandler, azureListResultFactory);
        this.postgreSqlManager = postgreSqlManager;
    }

    public void updateAdministratorLoginPassword(String resourceGroupName, String serverName, String newPassword) {
        Optional<Server> singleServer = getSingleServer(resourceGroupName, serverName);
        if (singleServer.isEmpty()) {
            String message = String.format("Single server not found with name %s in resource group %s", serverName, resourceGroupName);
            LOGGER.warn(message);
            throw new CloudConnectorException(message);
        } else {
            handleException(() -> singleServer.get().update().withAdministratorLoginPassword(newPassword).apply());
        }
    }

    public ServerState getSingleServerStatus(String resourceGroupName, String serverName) {
        Optional<Server> server = getSingleServer(resourceGroupName, serverName);
        if (server.isEmpty()) {
            LOGGER.debug("Single server not found with name {} in resourcegroup {}", serverName, resourceGroupName);
            return UNKNOWN;
        } else {
            return server.get().userVisibleState() != null ? server.get().userVisibleState() : UNKNOWN;
        }
    }

    public Optional<Server> getSingleServer(String resourceGroupName, String serverName) {
        return handleException(() -> postgreSqlManager.servers().getByResourceGroup(resourceGroupName, serverName));
    }
}
