package com.sequenceiq.cloudbreak.telemetry.logcollection;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.telemetry.databus.AbstractDatabusStreamConfiguration;

@Component
public class ClusterLogsCollectionConfiguration extends AbstractDatabusStreamConfiguration {

    public ClusterLogsCollectionConfiguration(
            @Value("${cluster.logs.collection.enabled:false}") boolean enabled,
            @Value("${cluster.logs.collection.dbus.app.name:}") String dbusAppName,
            @Value("${cluster.logs.collection.dbus.stream.name:LogCollection}") String dbusStreamName) {
        super(enabled, dbusAppName, dbusStreamName);
    }

    @Override
    public String getDbusServiceName() {
        return "ClusterLogsCollection";
    }

    @Override
    public String getDbusAppNameKey() {
        return "@logging-app";
    }
}
