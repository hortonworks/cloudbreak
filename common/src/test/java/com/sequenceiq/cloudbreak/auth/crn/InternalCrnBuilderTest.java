package com.sequenceiq.cloudbreak.auth.crn;

import static com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGenerator.regionalAwareInternalCrnGenerator;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class InternalCrnBuilderTest {

    @Test
    void testIsInternalCrnForServiceWhenServiceMatch() {
        RegionAwareInternalCrnGenerator autoscale = regionalAwareInternalCrnGenerator(Crn.Service.AUTOSCALE, "partition", "region");
        boolean autoscaleService = autoscale.isInternalCrnForService("crn:cdp:autoscale:us-west-1:altus:user:__internal__actor__");
        assertTrue(autoscaleService, "Internal Crn For Service should match.");
    }

    @Test
    void testIsInternalCrnForServiceWhenServiceNotMatching() {
        RegionAwareInternalCrnGenerator autoscale = regionalAwareInternalCrnGenerator(Crn.Service.AUTOSCALE, "partition", "region");
        boolean autoscaleService = autoscale.isInternalCrnForService("crn:cdp:datahub:us-west-1:altus:user:__internal__actor__");
        assertFalse(autoscaleService, "Internal Crn For Service should not match.");
    }

    @Test
    void testIsInternalCrnForServiceWhenNotInternalCrn() {
        RegionAwareInternalCrnGenerator autoscale = regionalAwareInternalCrnGenerator(Crn.Service.AUTOSCALE, "partition", "region");
        String testCrn = "crn:cdp:iam:us-west-1:9d74eee4-1cad-45d7-b645-7ccf9edbb73d:user:f3b8ed82-e712-4f89-bda7-be07183720d3";
        boolean autoscaleService = autoscale.isInternalCrnForService(testCrn);
        assertFalse(autoscaleService, "External Crn For Service should not match.");
    }
}