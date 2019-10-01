package com.sequenceiq.environment.environment.dto.telemetry;

public class EnvironmentWorkloadAnalytics extends CommonTelemetryParams {

    private String databusEndpoint;

    public String getDatabusEndpoint() {
        return databusEndpoint;
    }

    public void setDatabusEndpoint(String databusEndpoint) {
        this.databusEndpoint = databusEndpoint;
    }
}
