package com.sequenceiq.it.cloudbreak.newway.assertion.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.sequenceiq.it.cloudbreak.newway.assertion.AssertionV2;
import com.sequenceiq.it.cloudbreak.newway.dto.util.VersionCheckTestDto;

public class VersionCheckTestAssertion {

    private VersionCheckTestAssertion() {
    }

    public static AssertionV2<VersionCheckTestDto> versionIsNotOk() {
        return (testContext, entity, cloudbreakClient) -> {
            assertFalse(entity.getResponse().isVersionCheckOk());
            assertNotNull("Response message should be filled!", entity.getResponse().getMessage());
            assertTrue(entity.getResponse().getMessage().contains("not compatible"));
            return entity;
        };
    }

}
