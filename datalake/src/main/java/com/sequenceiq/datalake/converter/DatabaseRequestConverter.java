package com.sequenceiq.datalake.converter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.database.DatabaseAvailabilityType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.database.DatabaseAzureRequest;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.database.DatabaseRequest;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.common.model.AzureDatabaseType;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.entity.SdxDatabase;

@Component
public class DatabaseRequestConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseRequestConverter.class);

    public DatabaseRequest createExternalDbRequest(SdxCluster sdxCluster) {
        DatabaseRequest request = new DatabaseRequest();
        request.setAvailabilityType(DatabaseAvailabilityType.NONE);
        request.setDatabaseEngineVersion(sdxCluster.getDatabaseEngineVersion());
        request.setDatabaseAzureRequest(convertDatabaseAzureRequest(sdxCluster.getSdxDatabase()));
        LOGGER.debug("Created DB request: {}", request);
        return request;
    }

    private DatabaseAzureRequest convertDatabaseAzureRequest(SdxDatabase sdxDatabase) {
        if (sdxDatabase != null) {
            Json attributes = sdxDatabase.getAttributes();
            if (attributes != null) {
                Object azureDatabaseTypeObj = attributes.getMap().get(AzureDatabaseType.AZURE_DATABASE_TYPE_KEY);
                if (azureDatabaseTypeObj != null) {
                    DatabaseAzureRequest databaseAzureRequest = new DatabaseAzureRequest();
                    databaseAzureRequest.setAzureDatabaseType(AzureDatabaseType.safeValueOf(String.valueOf(azureDatabaseTypeObj)));
                    return databaseAzureRequest;
                }
            }
        }
        return null;
    }

}
