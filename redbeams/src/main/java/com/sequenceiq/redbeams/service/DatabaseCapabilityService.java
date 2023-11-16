package com.sequenceiq.redbeams.service;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudPlatformVariant;
import com.sequenceiq.cloudbreak.cloud.model.PlatformDatabaseCapabilities;
import com.sequenceiq.cloudbreak.cloud.model.Region;

@Component
public class DatabaseCapabilityService {
    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseCapabilityService.class);

    public String getDefaultInstanceType(CloudConnector connector, CloudCredential cloudCredential, CloudPlatformVariant cloudPlatformVariant,
            Region region) {
        try {
            PlatformDatabaseCapabilities databaseCapabilities = connector
                    .platformResources()
                    .databaseCapabilities(cloudCredential, region, Map.of());
            String instanceType = databaseCapabilities.getRegionDefaultInstanceTypeMap().get(region);
            LOGGER.debug("Default instancetype for database server is {} in {} region on {} cloud provider", instanceType, region, cloudPlatformVariant);
            return instanceType;
        } catch (UnsupportedOperationException exception) {
            LOGGER.warn("Database default instancetype determination is not supported on {} provider, returning null", cloudPlatformVariant, exception);
            return null;
        }
    }
}
