package com.sequenceiq.cloudbreak.service.database;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;

@Component
public class DatabaseDefaultVersionProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseDefaultVersionProvider.class);

    @Inject
    private DbOverrideConfig dbOverrideConfig;

    public String calculateDbVersionBasedOnRuntimeAndOsIfMissing(String runtime, String os, String requestedDbEngineVersion, CloudPlatform cloudPlatform,
            boolean externalDb, boolean flexibleServer) {
        if (StringUtils.isNotBlank(requestedDbEngineVersion)) {
            LOGGER.debug("DB engine version already requested to be [{}]", requestedDbEngineVersion);
            return requestedDbEngineVersion;
        } else if (StringUtils.isNotBlank(runtime)) {
            String dbEngineVersion = dbOverrideConfig.findEngineVersionForRuntime(runtime);
            dbEngineVersion = dbEngineVersion != null ? dbEngineVersion : dbOverrideConfig.findMinEngineVersion();
            LOGGER.debug("Setting DB engine version to [{}] for runtime [{}]", dbEngineVersion, runtime);
            return dbEngineVersion;
        } else {
            String dbEngineVersion = dbOverrideConfig.findMinEngineVersion();
            LOGGER.debug("Setting DB engine version to [{}] for os [{}]", dbEngineVersion, os);
            return dbEngineVersion;
        }
    }
}
