package com.sequenceiq.it.cloudbreak.assertion.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.sequenceiq.it.cloudbreak.assertion.Assertion;
import com.sequenceiq.it.cloudbreak.dto.util.VersionCheckTestDto;
import com.sequenceiq.it.cloudbreak.microservice.CloudbreakClient;

public class VersionCheckTestAssertion {

    private VersionCheckTestAssertion() {
    }

    public static Assertion<VersionCheckTestDto, CloudbreakClient> versionIsNotOk() {
        return (testContext, entity, cloudbreakClient) -> {
            assertFalse(entity.getResponse().isVersionCheckOk());
            assertNotNull("Response message should be filled!", entity.getResponse().getMessage());
            assertTrue(entity.getResponse().getMessage().contains("not compatible"));
            return entity;
        };
    }

}
