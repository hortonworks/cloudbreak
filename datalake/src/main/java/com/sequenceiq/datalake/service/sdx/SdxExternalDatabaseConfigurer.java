package com.sequenceiq.datalake.service.sdx;

import java.util.Comparator;
import java.util.Optional;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.DatabaseBase;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.database.DatabaseAvailabilityType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.database.DatabaseRequest;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.type.Versioned;
import com.sequenceiq.cloudbreak.service.database.DatabaseDefaultVersionProvider;
import com.sequenceiq.cloudbreak.util.VersionComparator;
import com.sequenceiq.datalake.configuration.PlatformConfig;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.entity.SdxDatabase;
import com.sequenceiq.datalake.service.sdx.database.AzureDatabaseAttributesService;
import com.sequenceiq.datalake.service.sdx.database.DatabaseParameterFallbackUtil;
import com.sequenceiq.sdx.api.model.SdxDatabaseAvailabilityType;
import com.sequenceiq.sdx.api.model.SdxDatabaseRequest;

@Component
public class SdxExternalDatabaseConfigurer {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxExternalDatabaseConfigurer.class);

    private static final String AZURE_EXT_DB_MIN_RUNTIME_VERSION = "7.1.0";

    @Value("${datalake.db.availability:HA}")
    private SdxDatabaseAvailabilityType defaultDatabaseAvailability;

    @Inject
    private PlatformConfig platformConfig;

    @Inject
    private DatabaseDefaultVersionProvider databaseDefaultVersionProvider;

    @Inject
    private AzureDatabaseAttributesService azureDatabaseAttributesService;

    private final Comparator<Versioned> versionComparator;

    public SdxExternalDatabaseConfigurer() {
        versionComparator = new VersionComparator();
    }

    public SdxDatabase configure(CloudPlatform cloudPlatform, String os, DatabaseRequest internalDatabaseRequest, SdxDatabaseRequest databaseRequest,
            SdxCluster sdxCluster) {
        LOGGER.debug("Create database configuration from internal request {} and database request {}", internalDatabaseRequest, databaseRequest);
        SdxDatabaseAvailabilityType databaseAvailabilityType = getDatabaseAvailabilityType(internalDatabaseRequest, databaseRequest, cloudPlatform, sdxCluster);
        String requestedDbEngineVersion = getDbEngineVersion(internalDatabaseRequest, databaseRequest);
        String dbEngineVersion =
                databaseDefaultVersionProvider.calculateDbVersionBasedOnRuntimeAndOsIfMissing(sdxCluster.getRuntime(), os, requestedDbEngineVersion);
        SdxDatabase sdxDatabase = DatabaseParameterFallbackUtil.setupDatabaseInitParams(sdxCluster, databaseAvailabilityType, dbEngineVersion);
        configureAzureDatabase(cloudPlatform, databaseRequest, sdxDatabase);
        LOGGER.debug("Set database availability type to {}, and engine version to {}", sdxCluster.getDatabaseAvailabilityType(),
                sdxCluster.getDatabaseEngineVersion());
        validate(cloudPlatform, sdxCluster);
        return sdxDatabase;
    }

    private String getDbEngineVersion(DatabaseRequest internalDatabaseRequest, SdxDatabaseRequest databaseRequest) {
        return Optional.ofNullable(databaseRequest)
                .map(SdxDatabaseRequest::getDatabaseEngineVersion)
                .orElse(Optional.ofNullable(internalDatabaseRequest)
                        .map(DatabaseBase::getDatabaseEngineVersion)
                        .orElse(null));
    }

    private SdxDatabaseAvailabilityType getDatabaseAvailabilityType(DatabaseRequest internalDatabaseRequest, SdxDatabaseRequest dbRequest,
            CloudPlatform cloudPlatform, SdxCluster sdxCluster) {
        Optional<SdxDatabaseAvailabilityType> availabilityType = getDatabaseAvailabilityType(internalDatabaseRequest, dbRequest);
        Optional<Boolean> createDatabase = Optional.ofNullable(dbRequest).map(SdxDatabaseRequest::getCreate);
        if (createDatabase.isEmpty() && availabilityType.isEmpty()) {
            if (platformConfig.isExternalDatabaseSupportedFor(cloudPlatform) && isCMExternalDbSupported(cloudPlatform, sdxCluster)) {
                return defaultDatabaseAvailability;
            } else {
                return SdxDatabaseAvailabilityType.NONE;
            }
        } else {
            return createDatabase.map(createDb -> Boolean.TRUE.equals(createDb) ? SdxDatabaseAvailabilityType.HA : SdxDatabaseAvailabilityType.NONE)
                    .orElseGet(availabilityType::get);
        }
    }

    private Optional<SdxDatabaseAvailabilityType> getDatabaseAvailabilityType(DatabaseRequest internalDatabaseRequest, SdxDatabaseRequest dbRequest) {
        return Optional.ofNullable(dbRequest).map(SdxDatabaseRequest::getAvailabilityType)
                .or(() -> Optional.ofNullable(internalDatabaseRequest).map(DatabaseBase::getAvailabilityType).map(this::convertAvailabilityType));
    }

    private SdxDatabaseAvailabilityType convertAvailabilityType(DatabaseAvailabilityType dbAvailabilityType) {
        switch (dbAvailabilityType) {
        case HA:
            return SdxDatabaseAvailabilityType.HA;
        case NON_HA:
            return SdxDatabaseAvailabilityType.NON_HA;
        default:
            return SdxDatabaseAvailabilityType.NONE;
        }
    }

    private boolean isCMExternalDbSupported(CloudPlatform cloudPlatform, SdxCluster sdxCluster) {
        if (CloudPlatform.AZURE == cloudPlatform) {
            String runtime = sdxCluster.getRuntime();
            if (StringUtils.isBlank(runtime)) {
                LOGGER.info("Runtime is not specified, external DB is permitted on Azure");
                return true;
            }
            boolean permitted = isVersionNewerOrEqualThan(sdxCluster::getRuntime, () -> AZURE_EXT_DB_MIN_RUNTIME_VERSION);
            LOGGER.info("External DB {} permitted on Azure with runtime version: {}", permitted ? "is" : "is NOT", runtime);
            return permitted;
        }
        return true;
    }

    private boolean isVersionNewerOrEqualThan(Versioned currentVersion, Versioned baseVersion) {
        LOGGER.info("Compared: version {} with new version {}", currentVersion.getVersion(), baseVersion.getVersion());
        return versionComparator.compare(currentVersion, baseVersion) > -1;
    }

    private void validate(CloudPlatform cloudPlatform, SdxCluster sdxCluster) {
        if (sdxCluster.hasExternalDatabase()
                && !platformConfig.isExternalDatabaseSupportedOrExperimental(cloudPlatform)) {
            String message = String.format("Cannot create external database for sdx: %s, for now only %s is/are supported", sdxCluster.getClusterName(),
                    platformConfig.getSupportedExternalDatabasePlatforms());
            LOGGER.debug(message);
            throw new BadRequestException(message);
        }
    }

    private void configureAzureDatabase(CloudPlatform cloudPlatform, SdxDatabaseRequest databaseRequest, SdxDatabase sdxDatabase) {
        if (CloudPlatform.AZURE == cloudPlatform) {
            azureDatabaseAttributesService.configureAzureDatabase(databaseRequest, sdxDatabase);
        }
    }
}
