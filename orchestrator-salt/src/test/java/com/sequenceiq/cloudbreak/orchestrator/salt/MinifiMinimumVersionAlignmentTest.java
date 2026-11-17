package com.sequenceiq.cloudbreak.orchestrator.salt;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.telemetry.TelemetryFeatureService;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;

class MinifiMinimumVersionAlignmentTest {

    private static final String TELEMETRY_SETTINGS_PATH = "salt-common/salt/telemetry/settings.sls";

    private static final String CEM_AGENTS_PACKAGE = "cem-agents";

    private static final String CDP_TELEMETRY_PACKAGE = "cdp-telemetry";

    private static final Pattern BUILD_NUMBER_PATTERN = Pattern.compile("^(.*[-_]b)(\\d+)$");

    private final TelemetryFeatureService telemetryFeatureService = new TelemetryFeatureService();

    private String saltMinifiMinVersion;

    private String saltCdpTelemetryMinVersion;

    @BeforeEach
    void setUp() throws IOException {
        String telemetrySettings = FileReaderUtils.readFileFromClasspath(TELEMETRY_SETTINGS_PATH);
        saltMinifiMinVersion = readSaltVariable(telemetrySettings, "minifi_min_version");
        saltCdpTelemetryMinVersion = readSaltVariable(telemetrySettings, "cdp_telemetry_min_version");
    }

    @Test
    void testJavaGateAcceptsSaltMinimumVersions() {
        assertTrue(telemetryFeatureService.isMinifiLoggingSupported(packages(saltMinifiMinVersion, saltCdpTelemetryMinVersion)));
    }

    @Test
    void testJavaGateRejectsMinifiVersionBelowSaltMinimum() {
        assertFalse(telemetryFeatureService.isMinifiLoggingSupported(packages(previousBuild(saltMinifiMinVersion), saltCdpTelemetryMinVersion)));
    }

    @Test
    void testJavaGateRejectsCdpTelemetryVersionBelowSaltMinimum() {
        assertFalse(telemetryFeatureService.isMinifiLoggingSupported(packages(saltMinifiMinVersion, previousBuild(saltCdpTelemetryMinVersion))));
    }

    private Map<String, String> packages(String cemAgentsVersion, String cdpTelemetryVersion) {
        return Map.of(CEM_AGENTS_PACKAGE, cemAgentsVersion, CDP_TELEMETRY_PACKAGE, cdpTelemetryVersion);
    }

    private String readSaltVariable(String telemetrySettings, String variableName) {
        Matcher matcher = Pattern.compile("\\{% set " + variableName + " = '([^']+)' %}").matcher(telemetrySettings);
        assertTrue(matcher.find(), variableName + " is not found in " + TELEMETRY_SETTINGS_PATH);
        return matcher.group(1);
    }

    private String previousBuild(String version) {
        Matcher matcher = BUILD_NUMBER_PATTERN.matcher(version);
        assertTrue(matcher.matches(), "Expected a version with a build number like 1.2.3-b4, but got " + version);
        int buildNumber = Integer.parseInt(matcher.group(2));
        assertTrue(buildNumber > 0, "Build number must be greater than zero: " + version);
        return matcher.group(1) + (buildNumber - 1);
    }
}
