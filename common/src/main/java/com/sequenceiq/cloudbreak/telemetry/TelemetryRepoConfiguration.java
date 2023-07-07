package com.sequenceiq.cloudbreak.telemetry;

import org.springframework.boot.context.properties.ConstructorBinding;

@ConstructorBinding
public record TelemetryRepoConfiguration(String name, String baseUrl, String gpgKey, Integer gpgCheck) {
}
