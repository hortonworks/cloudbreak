package com.sequenceiq.cloudbreak.converter.v4.stacks;

import static com.sequenceiq.cloudbreak.common.network.NetworkConstants.FLEXIBLE_SERVER_DELEGATED_SUBNET_ID;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.database.DatabaseAvailabilityType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.database.DatabaseAzureRequest;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.database.DatabaseRequest;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.stack.Database;
import com.sequenceiq.cloudbreak.service.database.EnvironmentDatabaseService;
import com.sequenceiq.common.model.AzureDatabaseType;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;

@Component
public class DatabaseRequestToDatabaseConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseRequestToDatabaseConverter.class);

    @Inject
    private EnvironmentDatabaseService environmentDatabaseService;

    public Database convert(DetailedEnvironmentResponse environment, CloudPlatform cloudPlatform, DatabaseRequest source) {
        Database database = new Database();
        if (source != null) {
            database.setExternalDatabaseAvailabilityType(Optional.ofNullable(source.getAvailabilityType()).orElse(DatabaseAvailabilityType.NONE));
            database.setExternalDatabaseEngineVersion(source.getDatabaseEngineVersion());
            database.setAttributes(configureAzureDatabaseIfNeeded(environment, cloudPlatform, source, source.getAvailabilityType()).orElse(null));
            database.setDatalakeDatabaseAvailabilityType(source.getDatalakeDatabaseAvailabilityType());
        }
        return database;
    }

    private Optional<Json> configureAzureDatabaseIfNeeded(DetailedEnvironmentResponse environment, CloudPlatform cloudPlatform, DatabaseRequest databaseRequest,
            DatabaseAvailabilityType availabilityType) {
        if (cloudPlatform == CloudPlatform.AZURE && availabilityType != null && !availabilityType.isEmbedded()) {
            AzureDatabaseType azureDatabaseType = Optional.ofNullable(databaseRequest)
                    .map(DatabaseRequest::getDatabaseAzureRequest)
                    .map(DatabaseAzureRequest::getAzureDatabaseType)
                    .orElse(null);
            azureDatabaseType = environmentDatabaseService.validateOrModifyDatabaseTypeIfNeeded(environment, azureDatabaseType);
            Optional<String> delegatedSubnetID = Optional.ofNullable(databaseRequest)
                    .map(DatabaseRequest::getDatabaseAzureRequest)
                    .map(DatabaseAzureRequest::getFlexibleServerDelegatedSubnetId);
            Map<String, Object> params = new HashMap<>();
            if (azureDatabaseType != null) {
                params.put(AzureDatabaseType.AZURE_DATABASE_TYPE_KEY, azureDatabaseType.name());
            }
            delegatedSubnetID.ifPresent(subnet -> params.put(FLEXIBLE_SERVER_DELEGATED_SUBNET_ID, subnet));
            return Optional.of(new Json(params));
        } else if (cloudPlatform == CloudPlatform.AZURE && databaseRequest.getDatabaseAzureRequest() != null
                && databaseRequest.getDatabaseAzureRequest().getAzureDatabaseType() != null) {
            Map<String, Object> params = createParamsWithDatabaseType(databaseRequest);
            addDelegatedSubnetId(databaseRequest, params);
            return Optional.of(new Json(params));
        } else {
            return Optional.empty();
        }
    }

    private static Map<String, Object> createParamsWithDatabaseType(DatabaseRequest databaseRequest) {
        Map<String, Object> params = new HashMap<>();
        params.put(AzureDatabaseType.AZURE_DATABASE_TYPE_KEY, databaseRequest.getDatabaseAzureRequest().getAzureDatabaseType().name());
        return params;
    }

    private static void addDelegatedSubnetId(DatabaseRequest databaseRequest, Map<String, Object> params) {
        String flexibleServerDelegatedSubnetId = databaseRequest.getDatabaseAzureRequest().getFlexibleServerDelegatedSubnetId();
        if (flexibleServerDelegatedSubnetId != null) {
            params.put(FLEXIBLE_SERVER_DELEGATED_SUBNET_ID, flexibleServerDelegatedSubnetId);
        }
    }
}
