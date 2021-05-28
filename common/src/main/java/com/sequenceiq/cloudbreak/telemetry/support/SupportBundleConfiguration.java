package com.sequenceiq.cloudbreak.telemetry.support;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.telemetry.databus.AbstractDatabusStreamConfiguration;

@Component
public class SupportBundleConfiguration extends AbstractDatabusStreamConfiguration {

    public SupportBundleConfiguration(
            @Value("${cluster.support.bundle.enabled:false}") boolean enabled,
            @Value("${cluster.support.bundle.dbus.app.name:}") String dbusAppName,
            @Value("${cluster.support.bundle.dbus.stream.name:UnifiedDiagnostics}") String dbusStreamName) {
        super(enabled, dbusAppName, dbusStreamName);
    }

    @Override
    public String getDbusServiceName() {
        return "UnifiedDiagnostics";
    }

    @Override
    public String getDbusAppNameKey() {
        return "unifieddiagnostics-app";
    }
}
