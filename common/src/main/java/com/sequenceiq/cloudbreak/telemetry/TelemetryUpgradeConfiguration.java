package com.sequenceiq.cloudbreak.telemetry;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties("telemetry.upgrade")
public class TelemetryUpgradeConfiguration {

    private boolean enabled;

    private TelemetryComponentUpgradeConfiguration cdpLoggingAgent;

    private TelemetryComponentUpgradeConfiguration cdpTelemetry;

    private TelemetryComponentUpgradeConfiguration meteringAgent;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public TelemetryComponentUpgradeConfiguration getCdpLoggingAgent() {
        return cdpLoggingAgent;
    }

    public void setCdpLoggingAgent(TelemetryComponentUpgradeConfiguration cdpLoggingAgent) {
        this.cdpLoggingAgent = cdpLoggingAgent;
    }

    public TelemetryComponentUpgradeConfiguration getCdpTelemetry() {
        return cdpTelemetry;
    }

    public void setCdpTelemetry(TelemetryComponentUpgradeConfiguration cdpTelemetry) {
        this.cdpTelemetry = cdpTelemetry;
    }

    public TelemetryComponentUpgradeConfiguration getMeteringAgent() {
        return meteringAgent;
    }

    public void setMeteringAgent(TelemetryComponentUpgradeConfiguration meteringAgent) {
        this.meteringAgent = meteringAgent;
    }
}
