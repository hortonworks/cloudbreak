package com.sequenceiq.common.api.telemetry.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.common.api.telemetry.common.CommonTelemetryParams;

@JsonIgnoreProperties(ignoreUnknown = true)
public class WorkloadAnalytics extends CommonTelemetryParams {

    @JsonProperty("databusEndpoint")
    private String databusEndpoint;

    public String getDatabusEndpoint() {
        return databusEndpoint;
    }

    public void setDatabusEndpoint(String databusEndpoint) {
        this.databusEndpoint = databusEndpoint;
    }
}
