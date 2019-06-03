package com.sequenceiq.cloudbreak.cloud.model;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonAutoDetect(fieldVisibility = ANY, getterVisibility = NONE, setterVisibility = NONE)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Telemetry {

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
