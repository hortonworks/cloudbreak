package com.sequenceiq.datalake.service.sdx;

import java.util.Comparator;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.VersionComparator;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.type.Versioned;
import com.sequenceiq.datalake.configuration.PlatformConfig;
import com.sequenceiq.datalake.controller.exception.BadRequestException;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.sdx.api.model.SdxDatabaseAvailabilityType;
import com.sequenceiq.sdx.api.model.SdxDatabaseRequest;

@Component
public class SdxExternalDatabaseConfigurer {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxExternalDatabaseConfigurer.class);

    private static final String AZURE_EXT_DB_MIN_RUNTIME_VERSION = "7.1.0";

    @Inject
    private PlatformConfig platformConfig;

    private final Comparator<Versioned> versionComparator;

    public SdxExternalDatabaseConfigurer() {
        versionComparator = new VersionComparator();
    }

    public void configure(CloudPlatform cloudPlatform, SdxDatabaseRequest databaseRequest, SdxCluster sdxCluster) {
        SdxDatabaseAvailabilityType databaseAvailabilityType = getDatabaseAvailabilityType(databaseRequest, cloudPlatform, sdxCluster);
        sdxCluster.setDatabaseAvailabilityType(databaseAvailabilityType);
        validate(cloudPlatform, sdxCluster);
    }

    private SdxDatabaseAvailabilityType getDatabaseAvailabilityType(SdxDatabaseRequest dbRequest, CloudPlatform cloudPlatform, SdxCluster sdxCluster) {
        if (dbRequest == null || (dbRequest.getCreate() == null && dbRequest.getAvailabilityType() == null)) {
            if (platformConfig.isExternalDatabaseSupportedFor(cloudPlatform) && isCMExternalDbSupported(cloudPlatform, sdxCluster)) {
                return SdxDatabaseAvailabilityType.HA;
            } else {
                return SdxDatabaseAvailabilityType.NONE;
            }
        } else {
            if (dbRequest.getCreate() == null) {
                return dbRequest.getAvailabilityType();
            } else {
                if (Boolean.TRUE.equals(dbRequest.getCreate())) {
                    return SdxDatabaseAvailabilityType.HA;
                } else {
                    return SdxDatabaseAvailabilityType.NONE;
                }
            }
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
}
