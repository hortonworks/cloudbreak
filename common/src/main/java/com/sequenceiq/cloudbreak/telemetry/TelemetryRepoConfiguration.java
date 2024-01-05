package com.sequenceiq.cloudbreak.telemetry;

public record TelemetryRepoConfiguration(String name, String baseUrl, String gpgKey, Integer gpgCheck) {
}
