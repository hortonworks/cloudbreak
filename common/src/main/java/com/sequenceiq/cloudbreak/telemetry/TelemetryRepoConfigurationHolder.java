package com.sequenceiq.cloudbreak.telemetry;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties("telemetry.repos")
public class TelemetryRepoConfigurationHolder extends AbstractTelemetryRepoConfigurationHolder {
}
