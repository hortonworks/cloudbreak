package com.sequenceiq.cloudbreak.cloud.gcp;

import static com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone.availabilityZone;
import static com.sequenceiq.cloudbreak.cloud.model.Region.region;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class GcpPlatformParametersTest {

    private GcpPlatformParameters p = new GcpPlatformParameters();

    @Test
    public void getAvailabilityZonesTest() {
        assertTrue(p.availabilityZones().getAllAvailabilityZone().contains(availabilityZone("us-east1-c")));
    }

    @Test
    public void getRegionsTest() {
        assertTrue(p.regions().types().contains(region("us-east1")));
    }

}