package com.sequenceiq.cloudbreak.telemetry;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TelemetryFeatureServiceTest {

    @InjectMocks
    private TelemetryFeatureService undertest;

    @Test
    void testIsECDSAAccessKeyTypeSupported() {
        boolean result = undertest.isECDSAAccessKeyTypeSupported(generatePackageVersions());
        assertTrue(result);
    }

    @Test
    void testIsECDSAAccessKeyTypeSupportedNull() {
        boolean result = undertest.isECDSAAccessKeyTypeSupported(null);
        assertFalse(result);
    }

    @Test
    void testIsECDSAAccessKeyTypeSupportedBadVersion() {
        Map<String, String> packages = new HashMap(generatePackageVersions());
        packages.put("cdp-logging-agent", "0.3.2");
        boolean result = undertest.isECDSAAccessKeyTypeSupported(packages);
        assertFalse(result);
    }

    @Test
    void testIsECDSAAccessKeyTypeSupportedMissingPackage() {
        Map<String, String> packages = new HashMap(generatePackageVersions());
        packages.remove("cdp-logging-agent");
        boolean result = undertest.isECDSAAccessKeyTypeSupported(packages);
        assertTrue(result);
    }

    @Test
    void testIsMinifiLoggingSupportedWhenPackagesIsNull() {
        assertFalse(undertest.isMinifiLoggingSupported(null));
    }

    @Test
    void testIsMinifiLoggingSupportedWhenPackagesDoesNotContainMinifi() {
        assertFalse(undertest.isMinifiLoggingSupported(Map.of()));
    }

    @Test
    void testIsMinifiLoggingSupportedWhenPackagesContainsOldMinifiVersion() {
        assertFalse(undertest.isMinifiLoggingSupported(Map.of("cdp-minifi-agent", "1.22.9")));
    }

    @Test
    void testIsMinifiLoggingSupportedWhenPackagesContainsCorrectMinifi() {
        assertTrue(undertest.isMinifiLoggingSupported(Map.of("cdp-minifi-agent", "1.25.09")));
    }

    private Map<String, String> generatePackageVersions() {
        return Map.of("cdp-logging-agent", "0.3.3",
                "cdp-request-signer", "0.2.3",
                "cdp-telemetry", "0.4.30");
    }

}