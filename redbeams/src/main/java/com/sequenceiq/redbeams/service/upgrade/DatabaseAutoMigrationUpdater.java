package com.sequenceiq.redbeams.service.upgrade;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.azure.resource.AzureRDSAutoMigrationException;
import com.sequenceiq.cloudbreak.cloud.azure.resource.AzureRDSAutoMigrationParams;
import com.sequenceiq.cloudbreak.cloud.exception.RdsAutoMigrationException;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.common.api.type.ResourceType;
import com.sequenceiq.common.model.AzureDatabaseType;
import com.sequenceiq.redbeams.domain.stack.DBResource;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.domain.stack.DatabaseServer;
import com.sequenceiq.redbeams.service.stack.DBResourceService;
import com.sequenceiq.redbeams.service.stack.DBStackService;

@Component
public class DatabaseAutoMigrationUpdater {
    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseAutoMigrationUpdater.class);

    @Inject
    private DBStackService dbStackService;

    @Inject
    private DBResourceService dbResourceService;

    public void updateDatabaseIfAutoMigrationHappened(DBStack dbStack, RdsAutoMigrationException rdsAutoMigrationException) {
        if (CloudPlatform.AZURE.name().equals(dbStack.getCloudPlatform()) &&
                rdsAutoMigrationException instanceof AzureRDSAutoMigrationException azureRDSAutoMigrationException) {
            Optional<AzureRDSAutoMigrationParams> azureRDSAutoMigrationParams = azureRDSAutoMigrationException.getAzureRDSAutoMigrationParams();
            azureRDSAutoMigrationParams.ifPresentOrElse(migrationParams -> updateDatabaseEntities(dbStack, azureRDSAutoMigrationException, migrationParams),
                    () -> LOGGER.debug("Auto migration params are empty, update is not possible on {} dbstack.", dbStack.getName()));
        }
    }

    private void updateDatabaseEntities(DBStack dbStack, AzureRDSAutoMigrationException azureRDSAutoMigrationException,
            AzureRDSAutoMigrationParams azureRDSAutoMigrationParams) {
        LOGGER.info("Automigration happened on Azure side from Single to Flexible server, the following params will be updated: {}",
                azureRDSAutoMigrationParams);
        Optional<DBResource> azureDbResource = dbResourceService.findByStackAndNameAndType(dbStack.getId(), dbStack.getDatabaseServer().getName(),
                ResourceType.AZURE_DATABASE);
        azureDbResource.ifPresentOrElse(dbResource -> updateDbResource(dbResource, azureRDSAutoMigrationParams),
                () -> LOGGER.warn("No db resource found for {}", dbStack.getDatabaseServer().getName()));

        DatabaseServer databaseServer = dbStack.getDatabaseServer();
        Json attributes = databaseServer.getAttributes();
        Map<String, Object> params = attributes == null ? new HashMap<>() : attributes.getMap();
        params.put(AzureDatabaseType.AZURE_DATABASE_TYPE_KEY, azureRDSAutoMigrationParams.azureDatabaseType().name());
        databaseServer.setAttributes(new Json(params));
        dbStackService.save(dbStack);
    }

    private void updateDbResource(DBResource dbResource, AzureRDSAutoMigrationParams azureRDSAutoMigrationParams) {
        dbResource.setResourceReference(azureRDSAutoMigrationParams.serverId());
        dbResourceService.save(dbResource);
    }
}
