package com.sequenceiq.cloudbreak.telemetry.orchestrator;

public enum TelemetryOrchestratorModule {

    TELEMETRY("telemetry"),
    FLUENT("fluent"),
    FILECOLLECTOR("filecollector"),
    DATABUS("databus"),
    MONITORING("monitoring");

    private final String value;

    TelemetryOrchestratorModule(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
