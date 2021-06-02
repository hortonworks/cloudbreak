package com.sequenceiq.cloudbreak.telemetry.monitoring;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.telemetry.databus.AbstractDatabusStreamConfiguration;

@Component
public class MonitoringConfiguration extends AbstractDatabusStreamConfiguration {

    public MonitoringConfiguration(
            @Value("${cluster.monitoring.enabled:true}") boolean enabled,
            @Value("${cluster.monitoring.dbus.app.name:CdpVmMetrics}") String dbusAppName,
            @Value("${cluster.monitoring.dbus.stream.name:CdpVmMetrics}") String dbusStreamName) {
        super(enabled, dbusAppName, dbusStreamName);
    }

    @Override
    public String getDbusServiceName() {
        return "Monitoring";
    }

    @Override
    public String getDbusAppNameKey() {
        return "@monitoring-app";
    }

}
