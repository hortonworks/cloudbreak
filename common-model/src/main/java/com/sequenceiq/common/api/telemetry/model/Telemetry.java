package com.sequenceiq.common.api.telemetry.model;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonAutoDetect(fieldVisibility = ANY, getterVisibility = NONE, setterVisibility = NONE)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Telemetry implements Serializable {

    private final Logging logging;

    private final WorkloadAnalytics workloadAnalytics;

    public Telemetry(@JsonProperty("logging") Logging logging,
            @JsonProperty("workloadAnalytics") WorkloadAnalytics workloadAnalytics) {
        this.logging = logging;
        this.workloadAnalytics = workloadAnalytics;
    }

    public Logging getLogging() {
        return logging;
    }

    public WorkloadAnalytics getWorkloadAnalytics() {
        return workloadAnalytics;
    }
}
