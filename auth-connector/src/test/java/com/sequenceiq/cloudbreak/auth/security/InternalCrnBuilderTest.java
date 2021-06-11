package com.sequenceiq.cloudbreak.auth.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.sequenceiq.cloudbreak.auth.altus.InternalCrnBuilder;
import com.sequenceiq.cloudbreak.auth.altus.Crn;

public class InternalCrnBuilderTest {

    private String userCrn = "crn:cdp:iam:us-west-1:9d74eee4-1cad-45d7-b645-7ccf9edbb73d:user:f3b8ed82-e712-4f89-bda7-be07183720d3";

    private String internalCrn = "crn:cdp:freeipa:us-west-1:altus:user:__internal__actor__";

    @Test
    public void generateInternalCrnWhenAutoscaleIsSpecified() {
        InternalCrnBuilder internalCrnBuilder = new InternalCrnBuilder(Crn.Service.AUTOSCALE);
        Crn generated = Crn.safeFromString(internalCrnBuilder.getInternalCrnForServiceAsString());
        assertEquals("altus", generated.getAccountId());
        assertEquals(Crn.Service.AUTOSCALE, generated.getService());
        assertEquals(Crn.ResourceType.USER, generated.getResourceType());
        assertEquals("__internal__actor__", generated.getResource());
    }

    @Test
    public void generateCrnAsStringWhenFreeIpaIs() {
        InternalCrnBuilder internalCrnBuilder = new InternalCrnBuilder(Crn.Service.FREEIPA);
        assertEquals("crn:cdp:freeipa:us-west-1:altus:user:__internal__actor__",
                internalCrnBuilder.getInternalCrnForServiceAsString());
    }

    @Test
    public void whenInternalCrnProvidedThenReturnTrue() {
        assertTrue(InternalCrnBuilder.isInternalCrn(internalCrn));
    }

    @Test
    public void whenUserCrnProvidedThenReturnFalse() {
        assertFalse(InternalCrnBuilder.isInternalCrn(userCrn));
    }
}
