package com.sequenceiq.cloudbreak.telemetry;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties("telemetry.devrepos")
public class DevTelemetryRepoConfigurationHolder extends AbstractTelemetryRepoConfigurationHolder {
}
