package com.sequenceiq.it.cloudbreak.assertion.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.it.cloudbreak.assertion.Assertion;
import com.sequenceiq.it.cloudbreak.dto.util.DeploymentPreferencesTestDto;
import com.sequenceiq.it.cloudbreak.microservice.CloudbreakClient;

public class DeploymentPreferencesTestAssertion {

    private DeploymentPreferencesTestAssertion() {
    }

    public static Assertion<DeploymentPreferencesTestDto, CloudbreakClient> supportedExternalDatabasesExists() {
        return (testContext, entity, cloudbreakClient) -> {
            assertNotNull(entity.getResponse().getSupportedExternalDatabases());
            assertFalse(entity.getResponse().getSupportedExternalDatabases().isEmpty());
            return entity;
        };
    }

    public static Assertion<DeploymentPreferencesTestDto, CloudbreakClient> platformEnablementValid() {
        return (testContext, entity, cloudbreakClient) -> {
            assertNotNull(entity.getResponse().getPlatformEnablement());
            assertFalse(entity.getResponse().getPlatformEnablement().isEmpty());
            Set<CloudPlatform> cloudPlatforms = Set.of(CloudPlatform.values());
            // OPENSTACK is deprecated now but we can not remove that for API compatibility reasons
            int platformSize = cloudPlatforms.size() - CloudPlatform.deprecated().size();
            assertEquals(platformSize, entity.getResponse().getPlatformEnablement().size());
            entity.getResponse().getPlatformEnablement().keySet().forEach(platform -> assertTrue(cloudPlatforms.contains(CloudPlatform.valueOf(platform))));
            return entity;
        };
    }

}
