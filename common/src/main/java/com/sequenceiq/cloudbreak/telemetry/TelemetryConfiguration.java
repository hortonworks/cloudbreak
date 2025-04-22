package com.sequenceiq.cloudbreak.telemetry;

import org.springframework.context.annotation.Configuration;

import com.sequenceiq.cloudbreak.altus.AltusDatabusConfiguration;
import com.sequenceiq.cloudbreak.telemetry.monitoring.MonitoringConfiguration;
import com.sequenceiq.cloudbreak.telemetry.support.SupportBundleConfiguration;

@Configuration
public class TelemetryConfiguration {

    private final AltusDatabusConfiguration altusDatabusConfiguration;

    private final MonitoringConfiguration monitoringConfiguration;

    private final SupportBundleConfiguration supportBundleConfiguration;

    public TelemetryConfiguration(AltusDatabusConfiguration altusDatabusConfiguration,
            MonitoringConfiguration monitoringConfiguration,
            SupportBundleConfiguration supportBundleConfiguration) {
        this.altusDatabusConfiguration = altusDatabusConfiguration;
        this.monitoringConfiguration = monitoringConfiguration;
        this.supportBundleConfiguration = supportBundleConfiguration;
    }

    public AltusDatabusConfiguration getAltusDatabusConfiguration() {
        return altusDatabusConfiguration;
    }

    public MonitoringConfiguration getMonitoringConfiguration() {
        return monitoringConfiguration;
    }

    public SupportBundleConfiguration getSupportBundleConfiguration() {
        return supportBundleConfiguration;
    }
}
