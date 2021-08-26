package com.sequenceiq.cloudbreak.telemetry;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class TelemetryVersionConfiguration {

    private final String desiredCdpTelemetryVersion;

    private final String desiredCdpLoggingAgentVersion;

    public TelemetryVersionConfiguration(@Value("${telemetry.cdpTelemetry.desiredVersion}") String desiredCdpTelemetryVersion,
            @Value("${telemetry.cdpLoggingAgent.desiredVersion}") String desiredCdpLoggingAgentVersion) {
        this.desiredCdpTelemetryVersion = desiredCdpTelemetryVersion;
        this.desiredCdpLoggingAgentVersion = desiredCdpLoggingAgentVersion;
    }

    public String getDesiredCdpTelemetryVersion() {
        return desiredCdpTelemetryVersion;
    }

    public String getDesiredCdpLoggingAgentVersion() {
        return desiredCdpLoggingAgentVersion;
    }
}
