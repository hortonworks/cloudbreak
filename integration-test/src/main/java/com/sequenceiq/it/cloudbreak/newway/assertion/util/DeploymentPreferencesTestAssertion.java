package com.sequenceiq.it.cloudbreak.newway.assertion.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Set;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.mappable.CloudPlatform;
import com.sequenceiq.it.cloudbreak.newway.assertion.AssertionV2;
import com.sequenceiq.it.cloudbreak.newway.dto.util.DeploymentPreferencesTestDto;

public class DeploymentPreferencesTestAssertion {

    private DeploymentPreferencesTestAssertion() {
    }

    public static AssertionV2<DeploymentPreferencesTestDto> supportedExternalDatabasesExists() {
        return (testContext, entity, cloudbreakClient) -> {
            assertNotNull(entity.getResponse().getSupportedExternalDatabases());
            assertFalse(entity.getResponse().getSupportedExternalDatabases().isEmpty());
            return entity;
        };
    }

    public static AssertionV2<DeploymentPreferencesTestDto> platformEnablementValid() {
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
