package com.sequenceiq.cloudbreak.telemetry.orchestrator;

public enum TelemetryOrchestratorModule {

    TELEMETRY("telemetry"),
    FLUENT("fluent"),
    FILECOLLECTOR("filecollector"),
    NODESTATUS("nodestatus"),
    DATABUS("databus"),
    MONITORING("monitoring"),
    METERING("metering");

    private final String value;

    TelemetryOrchestratorModule(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
