package com.sequenceiq.freeipa.flow.freeipa.provision.event.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.common.json.JsonUtil;

class InstallFreeIpaServicesRequestTest {

    @Test
    void testPayloadWithoutSkipFlagRunsPreServiceDeploymentRecipes() throws IOException {
        String payloadWithoutSkipFlag = "{\"@type\":\"" + InstallFreeIpaServicesRequest.class.getName() + "\",\"resourceId\":1}";

        InstallFreeIpaServicesRequest request = JsonUtil.readValue(payloadWithoutSkipFlag, InstallFreeIpaServicesRequest.class);

        assertEquals(1L, request.getResourceId());
        assertFalse(request.isSkipPreServiceDeploymentRecipes());
    }

    @Test
    void testSkipFlagSurvivesSerializationRoundTrip() throws IOException {
        String json = JsonUtil.writeValueAsString(new InstallFreeIpaServicesRequest(1L, true));

        assertTrue(JsonUtil.readValue(json, InstallFreeIpaServicesRequest.class).isSkipPreServiceDeploymentRecipes());
    }
}
