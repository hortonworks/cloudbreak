package com.sequenceiq.datalake.service.sdx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;

@ExtendWith(MockitoExtension.class)
class SdxVersionRuleEnforcerTest {

    private SdxVersionRuleEnforcer underTest;

    @BeforeEach
    public void setUp() {
        underTest = new SdxVersionRuleEnforcer();
        underTest.configure();
    }

    @Test
    void isRazSupportedForVersionTest() {
        assertTrue(underTest.isRazSupported(null, CloudPlatform.AWS), "Original implementation should accept null as runtime");
        assertTrue(underTest.isRazSupported(null, CloudPlatform.AZURE), "Original implementation should accept null as runtime");

        assertTrue(underTest.isRazSupported("7.2.2", CloudPlatform.AWS));
        assertTrue(underTest.isRazSupported("7.2.2", CloudPlatform.AZURE));
        assertTrue(underTest.isRazSupported("7.2.17", CloudPlatform.GCP));

        assertFalse(underTest.isRazSupported("7.2.1", CloudPlatform.AWS));
        assertFalse(underTest.isRazSupported("7.2.6", CloudPlatform.GCP));

    }

    @Test
    void getRazSupportedMinVersionTest() {
        assertEquals("7.2.2", underTest.getSupportedRazVersionForPlatform(CloudPlatform.AWS));
        assertEquals("7.2.2", underTest.getSupportedRazVersionForPlatform(CloudPlatform.AZURE));
        assertEquals("7.2.17", underTest.getSupportedRazVersionForPlatform(CloudPlatform.GCP));
    }
}