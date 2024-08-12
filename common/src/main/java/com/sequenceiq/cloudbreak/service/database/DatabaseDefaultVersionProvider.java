package com.sequenceiq.cloudbreak.service.database;

import static com.sequenceiq.common.model.OsType.RHEL8;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.util.VersionComparator;

@Component
public class DatabaseDefaultVersionProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseDefaultVersionProvider.class);

    @Value("${cb.db.override.minRuntimeVersion}")
    private String minRuntimeVersion;

    @Value("${cb.db.override.engineVersion}")
    private String dbEngineVersion;

    private final VersionComparator versionComparator = new VersionComparator();

    public String calculateDbVersionBasedOnRuntimeAndOsIfMissing(String runtime, String os, String requestedDbEngineVersion, CloudPlatform cloudPlatform,
            boolean externalDb, boolean flexibleServer) {
        if (StringUtils.isNotBlank(requestedDbEngineVersion)) {
            LOGGER.debug("DB engine version already requested to be [{}]", requestedDbEngineVersion);
            return requestedDbEngineVersion;
        } else if (StringUtils.isNotBlank(runtime)) {
            if (0 <= versionComparator.compare(() -> runtime, () -> minRuntimeVersion)) {
                LOGGER.debug("Setting DB engine version to [{}] for runtime [{}]", dbEngineVersion, runtime);
                return choosePg11InCaseOfAzureExternalDb(dbEngineVersion, cloudPlatform, externalDb, flexibleServer);
            } else {
                LOGGER.debug("Setting DB engine version to 'null' for runtime [{}]", runtime);
                return choosePg11InCaseOfAzureExternalDb(null, cloudPlatform, externalDb, flexibleServer);
            }
        } else if (RHEL8.getOs().equalsIgnoreCase(os)) {
            LOGGER.debug("Setting DB engine version to [{}] for os [{}]", dbEngineVersion, os);
            return choosePg11InCaseOfAzureExternalDb(dbEngineVersion, cloudPlatform, externalDb, flexibleServer);
        } else {
            LOGGER.warn("Setting DB engine version to 'null' for runtime [{}] and os [{}]", runtime, os);
            return choosePg11InCaseOfAzureExternalDb(null, cloudPlatform, externalDb, flexibleServer);
        }
    }

    private String choosePg11InCaseOfAzureExternalDb(String dbEngineVersion, CloudPlatform cloudPlatform, boolean externalDb, boolean flexibleEnabled) {
        if (CloudPlatform.AZURE == cloudPlatform && externalDb && !flexibleEnabled) {
            LOGGER.info("Selecting PG11 for Azure external DB");
            return "11";
        } else {
            return dbEngineVersion;
        }
    }
}
