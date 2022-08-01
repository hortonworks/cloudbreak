package com.sequenceiq.cloudbreak.telemetry.databus;

import java.util.HashMap;
import java.util.Map;

import com.sequenceiq.cloudbreak.telemetry.streaming.CommonStreamingConfiguration;

public abstract class AbstractDatabusStreamConfiguration extends CommonStreamingConfiguration {

    private final boolean enabled;

    private final String dbusAppName;

    private final String dbusStreamName;

    private final boolean streamingEnabled;

    public AbstractDatabusStreamConfiguration(boolean enabled, String dbusAppName, String dbusStreamName, boolean streamingEnabled) {
        this.enabled = enabled;
        this.dbusAppName = dbusAppName;
        this.dbusStreamName = dbusStreamName;
        this.streamingEnabled = streamingEnabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getDbusAppName() {
        return dbusAppName;
    }

    public String getDbusStreamName() {
        return dbusStreamName;
    }

    @Override
    public boolean isStreamingEnabled() {
        return streamingEnabled;
    }

    public abstract String getDbusServiceName();

    public abstract String getDbusAppNameKey();

    public Map<String, String> getDbusConfigs() {
        Map<String, String> map = new HashMap<>();
        map.put(String.format("dbus%sStreamName", getDbusServiceName()), dbusStreamName);
        map.put(String.format("dbus%sAppName", getDbusServiceName()), dbusAppName);
        return map;
    }

    @Override
    public String toString() {
        return "AbstractDatabusStreamConfiguration{" +
                "enabled=" + enabled +
                ", dbusAppName='" + dbusAppName + '\'' +
                ", dbusStreamName='" + dbusStreamName + '\'' +
                ", streamingEnabled=" + streamingEnabled +
                '}';
    }
}
