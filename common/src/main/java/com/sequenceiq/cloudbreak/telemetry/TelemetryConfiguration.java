package com.sequenceiq.cloudbreak.telemetry;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import com.sequenceiq.cloudbreak.altus.AltusDatabusConfiguration;

@Configuration
public class TelemetryConfiguration {

    private final AltusDatabusConfiguration altusDatabusConfiguration;

    private final boolean clusterLogsCollection;

    private final boolean meteringEnabled;

    private final boolean monitoringEnabled;

    public TelemetryConfiguration(AltusDatabusConfiguration altusDatabusConfiguration,
            @Value("${cluster.logs.collection.enabled:false}") boolean clusterLogsCollection,
            @Value("${metering.enabled:false}") boolean meteringEnabled,
            @Value("${cluster.monitoring.enabled:false}") boolean monitoringEnabled) {
        this.altusDatabusConfiguration = altusDatabusConfiguration;
        this.clusterLogsCollection = clusterLogsCollection;
        this.meteringEnabled = meteringEnabled;
        this.monitoringEnabled = monitoringEnabled;
    }

    public AltusDatabusConfiguration getAltusDatabusConfiguration() {
        return altusDatabusConfiguration;
    }

    public boolean isClusterLogsCollection() {
        return clusterLogsCollection;
    }

    public boolean isMeteringEnabled() {
        return meteringEnabled;
    }

    public boolean isMonitoringEnabled() {
        return monitoringEnabled;
    }
}
