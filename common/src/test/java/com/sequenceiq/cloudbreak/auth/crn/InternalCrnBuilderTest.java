package com.sequenceiq.cloudbreak.auth.crn;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class InternalCrnBuilderTest {

    @Test
    public void testIsInternalCrnForServiceWhenServiceMatch() {
        InternalCrnBuilder autoscale = new InternalCrnBuilder(Crn.Service.AUTOSCALE);
        boolean autoscaleService = InternalCrnBuilder.isInternalCrnForService(autoscale.getInternalCrnForServiceAsString(), Crn.Service.AUTOSCALE);
        assertTrue("Internal Crn For Service should match.", autoscaleService);
    }

    @Test
    public void testIsInternalCrnForServiceWhenServiceNotMatching() {
        InternalCrnBuilder autoscale = new InternalCrnBuilder(Crn.Service.DATAHUB);
        boolean autoscaleService = InternalCrnBuilder.isInternalCrnForService(autoscale.getInternalCrnForServiceAsString(), Crn.Service.AUTOSCALE);
        assertFalse("Internal Crn For Service should not match.", autoscaleService);
    }

    @Test
    public void testIsInternalCrnForServiceWhenNotInternalCrn() {
        String testCrn = "crn:cdp:iam:us-west-1:9d74eee4-1cad-45d7-b645-7ccf9edbb73d:user:f3b8ed82-e712-4f89-bda7-be07183720d3";
        boolean autoscaleService = InternalCrnBuilder.isInternalCrnForService(testCrn, Crn.Service.AUTOSCALE);
        assertFalse("External Crn For Service should not match.", autoscaleService);
    }
}