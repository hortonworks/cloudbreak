package com.sequenceiq.cloudbreak.cloud.gcp;

import org.junit.Assert;
import org.junit.Test;

import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Variant;

public class GcpConstantsTest {

    private GcpConstants underTest = new GcpConstants();

    @Test
    public void testPlatform() {
        Platform platform = underTest.platform();
        Assert.assertEquals(GcpConstants.GCP_PLATFORM, platform);
    }

    @Test
    public void testVariant() {
        Variant variant = underTest.variant();
        Assert.assertEquals(GcpConstants.GCP_VARIANT, variant);
    }

}