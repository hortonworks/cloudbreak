package com.sequenceiq.it.cloudbreak.newway.assertion.version;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.version.VersionCheckTestDto;

public class VersionCheckAssertion {

    private VersionCheckAssertion() {
    }

    public static VersionCheckTestDto versionIsNotOk(TestContext testContext, VersionCheckTestDto testDto, CloudbreakClient cloudbreakClient) {
        assertFalse(testDto.getResponse().isVersionCheckOk());
        assertNotNull("Response message should be filled!", testDto.getResponse().getMessage());
        assertTrue(testDto.getResponse().getMessage().contains("not compatible"));
        return testDto;
    }

}
