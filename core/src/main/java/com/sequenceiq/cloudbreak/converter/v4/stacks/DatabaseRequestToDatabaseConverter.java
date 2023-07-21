package com.sequenceiq.cloudbreak.converter.v4.stacks;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.database.DatabaseAzureRequest;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.database.DatabaseRequest;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.altus.model.Entitlement;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.stack.Database;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.common.model.AzureDatabaseType;

@Component
public class DatabaseRequestToDatabaseConverter {
    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseRequestToDatabaseConverter.class);

    @Inject
    private EntitlementService entitlementService;

    public Database convert(Stack stack, CloudPlatform cloudPlatform, DatabaseRequest source) {
        Database database = new Database();
        if (source != null) {
            database.setExternalDatabaseAvailabilityType(source.getAvailabilityType());
            database.setExternalDatabaseEngineVersion(source.getDatabaseEngineVersion());
            database.setAttributes(configureAzureDatabaseIfNeeded(cloudPlatform, source).orElse(null));
        }
        return database;
    }

    private Optional<Json> configureAzureDatabaseIfNeeded(CloudPlatform cloudPlatform, DatabaseRequest databaseRequest) {
        if (cloudPlatform == CloudPlatform.AZURE) {
            AzureDatabaseType azureDatabaseType = Optional.ofNullable(databaseRequest)
                    .map(DatabaseRequest::getDatabaseAzureRequest)
                    .map(DatabaseAzureRequest::getAzureDatabaseType)
                    .orElse(AzureDatabaseType.SINGLE_SERVER);
            String accountId = ThreadBasedUserCrnProvider.getAccountId();
            if (azureDatabaseType == AzureDatabaseType.FLEXIBLE_SERVER) {
                if (!entitlementService.isAzureDatabaseFlexibleServerEnabled(accountId)) {
                    LOGGER.info("Azure Flexible Database Server creation is not entitled for {} account.", accountId);
                    throw new BadRequestException("You are not entitled to use Flexible Database Server on Azure for your cluster." +
                            " Please contact Cloudera to enable " + Entitlement.CDP_AZURE_DATABASE_FLEXIBLE_SERVER + " for your account");
                }
            }
            Map<String, Object> params = new HashMap<>();
            params.put(AzureDatabaseType.AZURE_DATABASE_TYPE_KEY, azureDatabaseType.name());
            return Optional.of(new Json(params));
        } else {
            return Optional.empty();
        }
    }
}
