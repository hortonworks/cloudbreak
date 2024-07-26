package com.sequenceiq.cloudbreak.telemetry;

public enum TelemetryComponentType {
    CDP_TELEMETRY("cdp-telemetry"),
    CDP_LOGGING_AGENT("cdp-logging-agent");

    private final String value;

    TelemetryComponentType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
