package com.sequenceiq.cloudbreak.cloud.azure.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.azure.resourcemanager.postgresqlflexibleserver.PostgreSqlManager;
import com.azure.resourcemanager.postgresqlflexibleserver.models.Server;
import com.azure.resourcemanager.postgresqlflexibleserver.models.ServerState;
import com.sequenceiq.cloudbreak.cloud.azure.util.AzureExceptionHandler;

public class AzureFlexibleServerClient extends AbstractAzureServiceClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(AzureFlexibleServerClient.class);

    private final PostgreSqlManager postgreSqlFlexibleManager;

    public AzureFlexibleServerClient(PostgreSqlManager postgreSqlFlexibleManager, AzureExceptionHandler azureExceptionHandler) {
        super(azureExceptionHandler);
        this.postgreSqlFlexibleManager = postgreSqlFlexibleManager;
    }

    public void startFlexibleServer(String resourceGroupName, String serverName) {
        handleException(() -> postgreSqlFlexibleManager.servers().start(resourceGroupName, serverName));
    }

    public void stopFlexibleServer(String resourceGroupName, String serverName) {
        handleException(() -> postgreSqlFlexibleManager.servers().stop(resourceGroupName, serverName));
    }

    public ServerState getFlexibleServerStatus(String resourceGroupName, String serverName) {
        return handleException(() -> {
            Server server = postgreSqlFlexibleManager.servers()
                    .getByResourceGroup(resourceGroupName, serverName);
            if (server == null) {
                LOGGER.debug("Flexible server not found with name {} in resourcegroup {}", serverName, resourceGroupName);
                return null;
            } else {
                return server.state();
            }
        });
    }
}
