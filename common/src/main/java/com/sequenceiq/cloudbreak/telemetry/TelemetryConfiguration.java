package com.sequenceiq.cloudbreak.telemetry;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import com.sequenceiq.cloudbreak.altus.AltusDatabusConfiguration;

@Configuration
public class TelemetryConfiguration {

    private final AltusDatabusConfiguration altusDatabusConfiguration;

    private final boolean reportDeploymentLogs;

    private final boolean meteringEnabled;

    public TelemetryConfiguration(AltusDatabusConfiguration altusDatabusConfiguration,
            @Value("${cluster.deployment.logs.report:false}") boolean reportDeploymentLogs,
            @Value("${metering.enabled:false}") boolean meteringEnabled) {
        this.altusDatabusConfiguration = altusDatabusConfiguration;
        this.reportDeploymentLogs = reportDeploymentLogs;
        this.meteringEnabled = meteringEnabled;
    }

    public AltusDatabusConfiguration getAltusDatabusConfiguration() {
        return altusDatabusConfiguration;
    }

    public boolean isReportDeploymentLogs() {
        return reportDeploymentLogs;
    }

    public boolean isMeteringEnabled() {
        return meteringEnabled;
    }
}
