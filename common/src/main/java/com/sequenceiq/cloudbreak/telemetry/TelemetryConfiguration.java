package com.sequenceiq.cloudbreak.telemetry;

import org.springframework.context.annotation.Configuration;

import com.sequenceiq.cloudbreak.altus.AltusDatabusConfiguration;
import com.sequenceiq.cloudbreak.telemetry.metering.MeteringConfiguration;
import com.sequenceiq.cloudbreak.telemetry.monitoring.MonitoringConfiguration;
import com.sequenceiq.cloudbreak.telemetry.support.SupportBundleConfiguration;

@Configuration
public class TelemetryConfiguration {

    private final AltusDatabusConfiguration altusDatabusConfiguration;

    private final MeteringConfiguration meteringConfiguration;

    private final MonitoringConfiguration monitoringConfiguration;

    private final SupportBundleConfiguration supportBundleConfiguration;

    public TelemetryConfiguration(AltusDatabusConfiguration altusDatabusConfiguration,
            MeteringConfiguration meteringConfiguration,
            MonitoringConfiguration monitoringConfiguration,
            SupportBundleConfiguration supportBundleConfiguration) {
        this.altusDatabusConfiguration = altusDatabusConfiguration;
        this.meteringConfiguration = meteringConfiguration;
        this.monitoringConfiguration = monitoringConfiguration;
        this.supportBundleConfiguration = supportBundleConfiguration;
    }

    public AltusDatabusConfiguration getAltusDatabusConfiguration() {
        return altusDatabusConfiguration;
    }

    public MeteringConfiguration getMeteringConfiguration() {
        return meteringConfiguration;
    }

    public MonitoringConfiguration getMonitoringConfiguration() {
        return monitoringConfiguration;
    }

    public SupportBundleConfiguration getSupportBundleConfiguration() {
        return supportBundleConfiguration;
    }
}
