package com.sequenceiq.cloudbreak.cloud.azure.validator;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.azure.resourcemanager.postgresqlflexibleserver.models.Server;
import com.sequenceiq.cloudbreak.cloud.azure.AzureResourceGroupMetadataProvider;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.resource.AzureRDSAutoMigrationException;
import com.sequenceiq.cloudbreak.cloud.azure.resource.AzureRDSAutoMigrationParams;
import com.sequenceiq.cloudbreak.cloud.azure.view.AzureDatabaseServerView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.common.model.AzureDatabaseType;

@Component
public class AzureRDSAutoMigrationValidator {
    private static final Logger LOGGER = LoggerFactory.getLogger(AzureRDSAutoMigrationValidator.class);

    private static final String ERROR_MSG_TEMPLATE = "Automigration happened from Single to Flexible Server for the %s database server on Azure." +
            " Currently your database is Flexible Server with Postgres11." +
            " Database in-place upgrade to Flexible Server 14 will be initiated.";

    @Inject
    private AzureResourceGroupMetadataProvider azureResourceGroupMetadataProvider;

    public void validate(AuthenticatedContext authenticatedContext, DatabaseStack dbStack) {
        AzureDatabaseServerView azureDatabaseServerView = new AzureDatabaseServerView(dbStack.getDatabaseServer());
        if (azureDatabaseServerView.getAzureDatabaseType() == AzureDatabaseType.SINGLE_SERVER) {
            String resourceGroupName = azureResourceGroupMetadataProvider.getResourceGroupName(authenticatedContext.getCloudContext(), dbStack);
            AzureClient client = authenticatedContext.getParameter(AzureClient.class);
            Server server = getFlexibleServer(client, resourceGroupName, dbStack.getDatabaseServer().getServerId());
            if (server != null) {
                String errorMsg = String.format(ERROR_MSG_TEMPLATE, dbStack.getDatabaseServer().getServerId());
                LOGGER.warn(errorMsg);
                throw new AzureRDSAutoMigrationException(errorMsg, new AzureRDSAutoMigrationParams(AzureDatabaseType.FLEXIBLE_SERVER, server.id()));
            }
        }
    }

    private Server getFlexibleServer(AzureClient client, String resourceGroupName, String serverId) {
        try {
            return client.getFlexibleServerClient().getFlexibleServer(resourceGroupName, serverId);
        } catch (RuntimeException any) {
            LOGGER.warn("Exception during getting flexible server from azure", any);
            return null;
        }
    }
}
