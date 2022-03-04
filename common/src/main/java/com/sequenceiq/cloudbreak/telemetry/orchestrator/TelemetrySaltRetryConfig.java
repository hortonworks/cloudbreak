package com.sequenceiq.cloudbreak.telemetry.orchestrator;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties("telemetry.salt.retry")
public class TelemetrySaltRetryConfig {

    private int cloudStorageValidation;

    private int loggingAgentStop;

    private int nodeStatusCollect;

    private int diagnosticsCollect;

    private int meteringUpgrade;

    private int telemetryUpgrade;

    public int getCloudStorageValidation() {
        return cloudStorageValidation;
    }

    public void setCloudStorageValidation(int cloudStorageValidation) {
        this.cloudStorageValidation = cloudStorageValidation;
    }

    public int getLoggingAgentStop() {
        return loggingAgentStop;
    }

    public void setLoggingAgentStop(int loggingAgentStop) {
        this.loggingAgentStop = loggingAgentStop;
    }

    public int getNodeStatusCollect() {
        return nodeStatusCollect;
    }

    public void setNodeStatusCollect(int nodeStatusCollect) {
        this.nodeStatusCollect = nodeStatusCollect;
    }

    public int getDiagnosticsCollect() {
        return diagnosticsCollect;
    }

    public void setDiagnosticsCollect(int diagnosticsCollect) {
        this.diagnosticsCollect = diagnosticsCollect;
    }

    public int getMeteringUpgrade() {
        return meteringUpgrade;
    }

    public void setMeteringUpgrade(int meteringUpgrade) {
        this.meteringUpgrade = meteringUpgrade;
    }

    public int getTelemetryUpgrade() {
        return telemetryUpgrade;
    }

    public void setTelemetryUpgrade(int telemetryUpgrade) {
        this.telemetryUpgrade = telemetryUpgrade;
    }

    @Override
    public String toString() {
        return "TelemetrySaltRetryConfig{" +
                "cloudStorageValidation=" + cloudStorageValidation +
                ", loggingAgentStop=" + loggingAgentStop +
                ", nodeStatusCollect=" + nodeStatusCollect +
                ", diagnosticsCollect=" + diagnosticsCollect +
                ", meteringUpgrade=" + meteringUpgrade +
                ", telemetryUpgrade=" + telemetryUpgrade +
                '}';
    }
}
