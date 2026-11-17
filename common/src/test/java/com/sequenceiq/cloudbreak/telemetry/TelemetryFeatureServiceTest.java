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
        assertFalse(undertest.isMinifiLoggingSupported(minifiPackages("1.22.9-b40", "1.3.14_b2")));
    }

    @Test
    void testIsMinifiLoggingSupportedWhenPackagesContainsCorrectMinifi() {
        assertTrue(undertest.isMinifiLoggingSupported(minifiPackages("1.25.09-b39", "1.3.14_b2")));
    }

    @Test
    void testIsMinifiLoggingSupportedWhenCdpTelemetryIsMissing() {
        assertFalse(undertest.isMinifiLoggingSupported(Map.of("cem-agents", "1.25.09-b39")));
    }

    @Test
    void testIsMinifiLoggingSupportedWhenCemAgentsIsMissing() {
        assertFalse(undertest.isMinifiLoggingSupported(Map.of("cdp-telemetry", "1.3.14_b2")));
    }

    @Test
    void testIsMinifiLoggingSupportedWhenCdpTelemetryVersionIsTooOld() {
        assertFalse(undertest.isMinifiLoggingSupported(minifiPackages("1.25.09-b39", "1.3.14_b1")));
    }

    @Test
    void testIsMinifiLoggingSupportedWhenCdpTelemetryHasHigherBuildNumber() {
        assertTrue(undertest.isMinifiLoggingSupported(minifiPackages("1.25.09-b39", "1.3.14_b3")));
    }

    @Test
    void testIsMinifiLoggingSupportedWhenCdpTelemetryHasHigherPatchVersion() {
        assertTrue(undertest.isMinifiLoggingSupported(minifiPackages("1.25.09-b39", "1.3.15_b1")));
    }

    @Test
    void testIsMinifiLoggingSupportedWhenCdpTelemetryHasMultiDigitPatchVersion() {
        assertTrue(undertest.isMinifiLoggingSupported(minifiPackages("1.25.09-b39", "1.3.100_b1")));
    }

    @Test
    void testIsMinifiLoggingSupportedWhenCdpTelemetryHasSingleDigitPatchBelowMinimum() {
        assertFalse(undertest.isMinifiLoggingSupported(minifiPackages("1.25.09-b39", "1.3.9_b1")));
    }

    private Map<String, String> minifiPackages(String cemAgentsVersion, String cdpTelemetryVersion) {
        return Map.of("cem-agents", cemAgentsVersion,
                "cdp-telemetry", cdpTelemetryVersion);
    }

    private Map<String, String> generatePackageVersions() {
        return Map.of("cdp-logging-agent", "0.3.3",
                "cdp-request-signer", "0.2.3",
                "cdp-telemetry", "0.4.30");
    }

}