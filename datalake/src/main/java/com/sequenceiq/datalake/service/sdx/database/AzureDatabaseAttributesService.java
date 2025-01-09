package com.sequenceiq.datalake.service.sdx.database;

import static com.sequenceiq.cloudbreak.common.network.NetworkConstants.FLEXIBLE_SERVER_DELEGATED_SUBNET_ID;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.database.DatabaseAzureRequest;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.database.DatabaseRequest;
import com.sequenceiq.cloudbreak.common.database.MajorVersion;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.service.database.EnvironmentDatabaseService;
import com.sequenceiq.cloudbreak.util.VersionComparator;
import com.sequenceiq.common.model.AzureDatabaseType;
import com.sequenceiq.datalake.entity.SdxDatabase;
import com.sequenceiq.sdx.api.model.SdxDatabaseAzureRequest;
import com.sequenceiq.sdx.api.model.SdxDatabaseRequest;

@Component
public class AzureDatabaseAttributesService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureDatabaseAttributesService.class);

    @Inject
    private EnvironmentDatabaseService environmentDatabaseService;

    public AzureDatabaseType getAzureDatabaseType(SdxDatabase sdxDatabase) {
        String dbTypeStr = sdxDatabase.getAttributes() != null ?
                (String) sdxDatabase.getAttributes().getMap().get(AzureDatabaseType.AZURE_DATABASE_TYPE_KEY) : "";
        return getAzureDatabaseType(dbTypeStr);
    }

    public String getFlexibleServerDelegatedSubnetId(SdxDatabase sdxDatabase) {
        return Optional.ofNullable(sdxDatabase)
                .map(SdxDatabase::getAttributes)
                .map(Json::getMap)
                .map(attrMap -> (String) attrMap.get(FLEXIBLE_SERVER_DELEGATED_SUBNET_ID))
                .orElse(null);
    }

    public AzureDatabaseType determineAzureDatabaseType(DatabaseRequest internalDatabaseRequest,
            SdxDatabaseRequest databaseRequest) {
        AzureDatabaseType azureDatabaseType = Optional.ofNullable(databaseRequest)
                .map(SdxDatabaseRequest::getSdxDatabaseAzureRequest)
                .map(SdxDatabaseAzureRequest::getAzureDatabaseType)
                .orElse(Optional.ofNullable(internalDatabaseRequest)
                        .map(DatabaseRequest::getDatabaseAzureRequest)
                        .map(DatabaseAzureRequest::getAzureDatabaseType)
                        .orElse(null));
        azureDatabaseType = environmentDatabaseService.validateOrModifyDatabaseTypeIfNeeded(azureDatabaseType);
        return azureDatabaseType;
    }

    public void configureAzureDatabase(AzureDatabaseType azureDatabaseType, DatabaseRequest internalDatabaseRequest,
            SdxDatabaseRequest databaseRequest, SdxDatabase sdxDatabase) {
        Json attributes = sdxDatabase.getAttributes();
        Map<String, Object> params = attributes == null ? new HashMap<>() : attributes.getMap();
        if (azureDatabaseType != null) {
            params.put(AzureDatabaseType.AZURE_DATABASE_TYPE_KEY, azureDatabaseType);
        }
        getFlexibleServerDelegatedSubnetId(internalDatabaseRequest, databaseRequest)
                .ifPresent(subnetId -> params.put(FLEXIBLE_SERVER_DELEGATED_SUBNET_ID, subnetId));
        sdxDatabase.setAttributes(new Json(params));
    }

    private Optional<String> getFlexibleServerDelegatedSubnetId(DatabaseRequest internalDatabaseRequest, SdxDatabaseRequest databaseRequest) {
        return Optional.ofNullable(databaseRequest)
                .map(SdxDatabaseRequest::getSdxDatabaseAzureRequest)
                .map(SdxDatabaseAzureRequest::getFlexibleServerDelegatedSubnetId)
                .or(() -> Optional.ofNullable(internalDatabaseRequest)
                        .map(DatabaseRequest::getDatabaseAzureRequest)
                        .map(DatabaseAzureRequest::getFlexibleServerDelegatedSubnetId));
    }

    public Optional<SdxDatabase> updateVersionRelatedDatabaseParams(SdxDatabase sdxDatabase, String dbVersion) {
        VersionComparator versionComparator = new VersionComparator();
        Map<String, Object> attributes = sdxDatabase.getAttributes() != null ? sdxDatabase.getAttributes().getMap() : new HashMap<>();
        AzureDatabaseType dbType = getAzureDatabaseType((String) attributes.getOrDefault(AzureDatabaseType.AZURE_DATABASE_TYPE_KEY, ""));
        boolean updateNeeded = versionComparator.compare(() -> dbVersion, MajorVersion.VERSION_14::getMajorVersion) >= 0 &&
                dbType != AzureDatabaseType.FLEXIBLE_SERVER;
        if (updateNeeded) {
            LOGGER.debug("Azure database type is updated to FLEXIBLE_SERVER, because of db version {}", dbVersion);
            attributes.put(AzureDatabaseType.AZURE_DATABASE_TYPE_KEY, AzureDatabaseType.FLEXIBLE_SERVER);
            sdxDatabase.setAttributes(new Json(attributes));
            return Optional.of(sdxDatabase);
        } else {
            LOGGER.debug("No Azure databasetype update is needed. Azure dbtype: {}, db version: {}", dbType, dbVersion);
            return Optional.empty();
        }
    }

    public SdxDatabase updateDatabaseType(SdxDatabase sdxDatabase, AzureDatabaseType azureDatabaseType) {
        Map<String, Object> attributes = sdxDatabase.getAttributes() != null ? sdxDatabase.getAttributes().getMap() : new HashMap<>();
        attributes.put(AzureDatabaseType.AZURE_DATABASE_TYPE_KEY, azureDatabaseType);
        sdxDatabase.setAttributes(new Json(attributes));
        return sdxDatabase;
    }

    private AzureDatabaseType getAzureDatabaseType(String dbTypeStr) {
        if (StringUtils.isNotBlank(dbTypeStr)) {
            return AzureDatabaseType.safeValueOf(dbTypeStr);
        } else {
            return AzureDatabaseType.SINGLE_SERVER;
        }
    }
}
