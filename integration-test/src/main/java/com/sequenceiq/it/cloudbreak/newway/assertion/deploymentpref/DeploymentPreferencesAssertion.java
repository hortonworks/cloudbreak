package com.sequenceiq.it.cloudbreak.newway.assertion.deploymentpref;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Set;

import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.providers.CloudPlatform;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.deploymentpref.DeploymentPreferencesTestDto;

public class DeploymentPreferencesAssertion {

    private DeploymentPreferencesAssertion() {
    }

    public static DeploymentPreferencesTestDto supportedExternalDatabasesExists(TestContext tc, DeploymentPreferencesTestDto entity, CloudbreakClient cc) {
        assertNotNull(entity.getResponse().getSupportedExternalDatabases());
        assertFalse(entity.getResponse().getSupportedExternalDatabases().isEmpty());
        return entity;
    }

    public static DeploymentPreferencesTestDto platformEnablementValid(TestContext tc, DeploymentPreferencesTestDto entity, CloudbreakClient cc) {
        assertNotNull(entity.getResponse().getPlatformEnablement());
        assertFalse(entity.getResponse().getPlatformEnablement().isEmpty());
        var cloudPlatforms = Set.of(CloudPlatform.values());
        assertEquals(cloudPlatforms.size(), entity.getResponse().getPlatformEnablement().size());
        entity.getResponse().getPlatformEnablement().keySet().forEach(platform -> assertTrue(cloudPlatforms.contains(CloudPlatform.valueOf(platform))));
        return entity;
    }

}
