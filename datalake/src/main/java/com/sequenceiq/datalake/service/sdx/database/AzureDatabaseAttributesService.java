package com.sequenceiq.datalake.service.sdx.database;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.database.DatabaseAzureRequest;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.database.DatabaseRequest;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.service.database.EnvironmentDatabaseService;
import com.sequenceiq.common.model.AzureDatabaseType;
import com.sequenceiq.datalake.entity.SdxDatabase;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.sdx.api.model.SdxDatabaseAzureRequest;
import com.sequenceiq.sdx.api.model.SdxDatabaseRequest;

@Component
public class AzureDatabaseAttributesService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureDatabaseAttributesService.class);

    @Inject
    private EntitlementService entitlementService;

    @Inject
    private EnvironmentDatabaseService environmentDatabaseService;

    public AzureDatabaseType getAzureDatabaseType(SdxDatabase sdxDatabase) {
        String dbTypeStr = sdxDatabase.getAttributes() != null ?
                (String) sdxDatabase.getAttributes().getMap().get(AzureDatabaseType.AZURE_DATABASE_TYPE_KEY) : "";
        if (StringUtils.isNotBlank(dbTypeStr)) {
            return AzureDatabaseType.safeValueOf(dbTypeStr);
        } else {
            return AzureDatabaseType.SINGLE_SERVER;
        }
    }

    public AzureDatabaseType determineAzureDatabaseType(DetailedEnvironmentResponse environment, DatabaseRequest internalDatabaseRequest,
            SdxDatabaseRequest databaseRequest) {
        AzureDatabaseType azureDatabaseType = Optional.ofNullable(databaseRequest)
                .map(SdxDatabaseRequest::getSdxDatabaseAzureRequest)
                .map(SdxDatabaseAzureRequest::getAzureDatabaseType)
                .orElse(Optional.ofNullable(internalDatabaseRequest)
                        .map(DatabaseRequest::getDatabaseAzureRequest)
                        .map(DatabaseAzureRequest::getAzureDatabaseType)
                        .orElse(null));
        azureDatabaseType = environmentDatabaseService.validateOrModifyDatabaseTypeIfNeeded(environment, azureDatabaseType);
        return azureDatabaseType;
    }

    public void configureAzureDatabase(AzureDatabaseType azureDatabaseType, SdxDatabase sdxDatabase) {
        Json attributes = sdxDatabase.getAttributes();
        Map<String, Object> params = attributes == null ? new HashMap<>() : attributes.getMap();
        if (azureDatabaseType != null) {
            params.put(AzureDatabaseType.AZURE_DATABASE_TYPE_KEY, azureDatabaseType);
        }
        sdxDatabase.setAttributes(new Json(params));
    }
}
