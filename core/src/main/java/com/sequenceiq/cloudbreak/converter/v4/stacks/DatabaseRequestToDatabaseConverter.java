package com.sequenceiq.cloudbreak.converter.v4.stacks;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.database.DatabaseRequest;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.stack.Database;
import com.sequenceiq.common.model.AzureDatabaseType;

@Component
public class DatabaseRequestToDatabaseConverter {
    @Inject
    private EntitlementService entitlementService;

    public Database convert(CloudPlatform cloudPlatform, DatabaseRequest source) {
        Database database = new Database();
        if (source != null) {
            database.setExternalDatabaseAvailabilityType(source.getAvailabilityType());
            database.setExternalDatabaseEngineVersion(source.getDatabaseEngineVersion());
            database.setAttributes(configureAzureDatabaseIfNeeded(cloudPlatform).orElse(null));
        }
        return database;
    }

    private Optional<Json> configureAzureDatabaseIfNeeded(CloudPlatform cloudPlatform) {
        if (cloudPlatform == CloudPlatform.AZURE) {
            String accountId = ThreadBasedUserCrnProvider.getAccountId();
            AzureDatabaseType azureDatabaseType = entitlementService.isAzureDatabaseFlexibleServerEnabled(accountId) ? AzureDatabaseType.FLEXIBLE_SERVER :
                    AzureDatabaseType.SINGLE_SERVER;
            Map<String, Object> params = new HashMap<>();
            params.put(AzureDatabaseType.AZURE_DATABASE_TYPE_KEY, azureDatabaseType.name());
            return Optional.of(new Json(params));
        } else {
            return Optional.empty();
        }
    }
}
