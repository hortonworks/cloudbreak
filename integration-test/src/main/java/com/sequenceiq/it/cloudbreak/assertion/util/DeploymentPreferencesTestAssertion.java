package com.sequenceiq.it.cloudbreak.assertion.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Set;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.assertion.Assertion;
import com.sequenceiq.it.cloudbreak.dto.util.DeploymentPreferencesTestDto;

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
            var cloudPlatforms = Set.of(CloudPlatform.values());
            assertEquals(cloudPlatforms.size(), entity.getResponse().getPlatformEnablement().size());
            entity.getResponse().getPlatformEnablement().keySet().forEach(platform -> assertTrue(cloudPlatforms.contains(CloudPlatform.valueOf(platform))));
            return entity;
        };
    }

}
