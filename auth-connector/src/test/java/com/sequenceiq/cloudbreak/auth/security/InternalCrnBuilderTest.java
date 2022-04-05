package com.sequenceiq.cloudbreak.auth.security;

import static com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGenerator.regionalAwareInternalCrnGenerator;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGenerator;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorUtil;

public class InternalCrnBuilderTest {

    private String userCrn = "crn:cdp:iam:us-west-1:9d74eee4-1cad-45d7-b645-7ccf9edbb73d:user:f3b8ed82-e712-4f89-bda7-be07183720d3";

    private String internalCrn = "crn:cdp:freeipa:us-west-1:altus:user:__internal__actor__";

    @Test
    public void generateInternalCrnWhenAutoscaleIsSpecified() {
        RegionAwareInternalCrnGenerator regionAwareInternalCrnGenerator =
                regionalAwareInternalCrnGenerator(Crn.Service.AUTOSCALE, "cdp", "us-west-1");
        Crn generated = Crn.safeFromString(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString());
        assertEquals("altus", generated.getAccountId());
        assertEquals(Crn.Service.AUTOSCALE, generated.getService());
        assertEquals(Crn.ResourceType.USER, generated.getResourceType());
        assertEquals("__internal__actor__", generated.getResource());
    }

    @Test
    public void generateCrnAsStringWhenFreeIpaIs() {
        RegionAwareInternalCrnGenerator regionAwareInternalCrnGenerator =
                regionalAwareInternalCrnGenerator(Crn.Service.FREEIPA, "cdp", "us-west-1");
        assertEquals("crn:cdp:freeipa:us-west-1:altus:user:__internal__actor__",
                regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString());
    }

    @Test
    public void whenInternalCrnProvidedThenReturnTrue() {
        assertTrue(RegionAwareInternalCrnGeneratorUtil.isInternalCrn(internalCrn));
    }

    @Test
    public void whenUserCrnProvidedThenReturnFalse() {
        assertFalse(RegionAwareInternalCrnGeneratorUtil.isInternalCrn(userCrn));
    }
}
