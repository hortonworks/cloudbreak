package com.sequenceiq.cloudbreak.telemetry;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import com.sequenceiq.cloudbreak.altus.AltusDatabusConfiguration;
import com.sequenceiq.cloudbreak.telemetry.logcollection.ClusterLogsCollectionConfiguration;
import com.sequenceiq.cloudbreak.telemetry.metering.MeteringConfiguration;

@Configuration
public class TelemetryConfiguration {

    private final AltusDatabusConfiguration altusDatabusConfiguration;

    private final MeteringConfiguration meteringConfiguration;

    private final ClusterLogsCollectionConfiguration clusterLogsCollectionConfiguration;

    private final boolean monitoringEnabled;

    public TelemetryConfiguration(AltusDatabusConfiguration altusDatabusConfiguration,
            MeteringConfiguration meteringConfiguration,
            ClusterLogsCollectionConfiguration clusterLogsCollectionConfiguration,
            @Value("${cluster.monitoring.enabled:false}") boolean monitoringEnabled) {
        this.altusDatabusConfiguration = altusDatabusConfiguration;
        this.meteringConfiguration = meteringConfiguration;
        this.clusterLogsCollectionConfiguration = clusterLogsCollectionConfiguration;
        this.monitoringEnabled = monitoringEnabled;
    }

    public AltusDatabusConfiguration getAltusDatabusConfiguration() {
        return altusDatabusConfiguration;
    }

    public MeteringConfiguration getMeteringConfiguration() {
        return meteringConfiguration;
    }

    public ClusterLogsCollectionConfiguration getClusterLogsCollectionConfiguration() {
        return clusterLogsCollectionConfiguration;
    }

    public boolean isMonitoringEnabled() {
        return monitoringEnabled;
    }
}
