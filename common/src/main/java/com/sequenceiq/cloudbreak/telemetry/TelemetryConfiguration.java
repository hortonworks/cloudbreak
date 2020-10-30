package com.sequenceiq.cloudbreak.telemetry;

import org.springframework.context.annotation.Configuration;

import com.sequenceiq.cloudbreak.altus.AltusDatabusConfiguration;
import com.sequenceiq.cloudbreak.telemetry.logcollection.ClusterLogsCollectionConfiguration;
import com.sequenceiq.cloudbreak.telemetry.metering.MeteringConfiguration;
import com.sequenceiq.cloudbreak.telemetry.monitoring.MonitoringConfiguration;

@Configuration
public class TelemetryConfiguration {

    private final AltusDatabusConfiguration altusDatabusConfiguration;

    private final MeteringConfiguration meteringConfiguration;

    private final ClusterLogsCollectionConfiguration clusterLogsCollectionConfiguration;

    private final MonitoringConfiguration monitoringConfiguration;

    public TelemetryConfiguration(AltusDatabusConfiguration altusDatabusConfiguration,
            MeteringConfiguration meteringConfiguration,
            ClusterLogsCollectionConfiguration clusterLogsCollectionConfiguration,
            MonitoringConfiguration monitoringConfiguration) {
        this.altusDatabusConfiguration = altusDatabusConfiguration;
        this.meteringConfiguration = meteringConfiguration;
        this.clusterLogsCollectionConfiguration = clusterLogsCollectionConfiguration;
        this.monitoringConfiguration = monitoringConfiguration;
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

    public MonitoringConfiguration getMonitoringConfiguration() {
        return monitoringConfiguration;
    }
}
