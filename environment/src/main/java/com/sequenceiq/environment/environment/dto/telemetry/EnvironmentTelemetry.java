package com.sequenceiq.environment.environment.dto.telemetry;

import java.io.Serializable;

public class EnvironmentTelemetry implements Serializable {

    private final EnvironmentLogging logging;

    private final EnvironmentWorkloadAnalytics workloadAnalytics;

    public EnvironmentTelemetry(EnvironmentLogging logging, EnvironmentWorkloadAnalytics workloadAnalytics) {
        this.logging = logging;
        this.workloadAnalytics = workloadAnalytics;
    }

    public EnvironmentLogging getLogging() {
        return logging;
    }

    public EnvironmentWorkloadAnalytics getWorkloadAnalytics() {
        return workloadAnalytics;
    }
}
