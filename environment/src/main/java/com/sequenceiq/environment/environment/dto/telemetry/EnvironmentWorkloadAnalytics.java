package com.sequenceiq.environment.environment.dto.telemetry;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class EnvironmentWorkloadAnalytics extends CommonTelemetryParams {

    private String databusEndpoint;

    public String getDatabusEndpoint() {
        return databusEndpoint;
    }

    public void setDatabusEndpoint(String databusEndpoint) {
        this.databusEndpoint = databusEndpoint;
    }
}
