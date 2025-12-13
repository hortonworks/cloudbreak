package com.sequenceiq.cloudbreak.cloud.gcp;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Variant;

class GcpConstantsTest {

    private GcpConstants underTest = new GcpConstants();

    @Test
    void testPlatform() {
        Platform platform = underTest.platform();
        assertEquals(GcpConstants.GCP_PLATFORM, platform);
    }

    @Test
    void testVariant() {
        Variant variant = underTest.variant();
        assertEquals(GcpConstants.GCP_VARIANT, variant);
    }

}