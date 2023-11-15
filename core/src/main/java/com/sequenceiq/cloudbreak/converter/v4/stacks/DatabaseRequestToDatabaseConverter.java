package com.sequenceiq.cloudbreak.converter.v4.stacks;

import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.database.DatabaseAvailabilityType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.database.DatabaseAzureRequest;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.database.DatabaseRequest;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.altus.model.Entitlement;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.stack.Database;
import com.sequenceiq.common.model.AzureDatabaseType;

@Component
public class DatabaseRequestToDatabaseConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseRequestToDatabaseConverter.class);

    @Inject
    private EntitlementService entitlementService;

    public Database convert(CloudPlatform cloudPlatform, DatabaseRequest source) {
        Database database = new Database();
        if (source != null) {
            database.setExternalDatabaseAvailabilityType(Optional.ofNullable(source.getAvailabilityType()).orElse(DatabaseAvailabilityType.NONE));
            database.setExternalDatabaseEngineVersion(source.getDatabaseEngineVersion());
            database.setAttributes(configureAzureDatabaseIfNeeded(cloudPlatform, source, source.getAvailabilityType()).orElse(null));
            database.setDatalakeDatabaseAvailabilityType(source.getDatalakeDatabaseAvailabilityType());
        }
        return database;
    }

    private Optional<Json> configureAzureDatabaseIfNeeded(CloudPlatform cloudPlatform, DatabaseRequest databaseRequest,
            DatabaseAvailabilityType availabilityType) {
        if (cloudPlatform == CloudPlatform.AZURE && availabilityType != null && !availabilityType.isEmbedded()) {
            String accountId = ThreadBasedUserCrnProvider.getAccountId();
            boolean azureDatabaseFlexibleServerEnabled = entitlementService.isAzureDatabaseFlexibleServerEnabled(accountId);
            AzureDatabaseType azureDatabaseType = Optional.ofNullable(databaseRequest)
                    .map(DatabaseRequest::getDatabaseAzureRequest)
                    .map(DatabaseAzureRequest::getAzureDatabaseType)
                    .orElse(azureDatabaseFlexibleServerEnabled ? AzureDatabaseType.FLEXIBLE_SERVER : AzureDatabaseType.SINGLE_SERVER);
            if (azureDatabaseType == AzureDatabaseType.FLEXIBLE_SERVER && !azureDatabaseFlexibleServerEnabled) {
                LOGGER.info("Azure Flexible Database Server creation is not entitled for {} account", accountId);
                throw new BadRequestException("You are not entitled to use Flexible Database Server on Azure for your cluster." +
                        " Please contact Cloudera to enable " + Entitlement.CDP_AZURE_DATABASE_FLEXIBLE_SERVER + " for your account");
            }
            Map<String, Object> params = Map.of(AzureDatabaseType.AZURE_DATABASE_TYPE_KEY, azureDatabaseType.name());
            return Optional.of(new Json(params));
        } else if (cloudPlatform == CloudPlatform.AZURE && databaseRequest.getDatabaseAzureRequest() != null
                && databaseRequest.getDatabaseAzureRequest().getAzureDatabaseType() != null) {
            Map<String, Object> params = Map.of(AzureDatabaseType.AZURE_DATABASE_TYPE_KEY,
                    databaseRequest.getDatabaseAzureRequest().getAzureDatabaseType().name());
            return Optional.of(new Json(params));
        } else {
            return Optional.empty();
        }
    }
}
