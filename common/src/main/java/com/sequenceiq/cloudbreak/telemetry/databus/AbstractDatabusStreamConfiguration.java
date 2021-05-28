package com.sequenceiq.cloudbreak.telemetry.databus;

import java.util.HashMap;
import java.util.Map;

public abstract class AbstractDatabusStreamConfiguration {

    private final boolean enabled;

    private final String dbusAppName;

    private final String dbusStreamName;

    public AbstractDatabusStreamConfiguration(boolean enabled, String dbusAppName, String dbusStreamName) {
        this.enabled = enabled;
        this.dbusAppName = dbusAppName;
        this.dbusStreamName = dbusStreamName;
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

    public abstract String getDbusServiceName();

    public abstract String getDbusAppNameKey();

    public Map<String, String> getDbusConfigs() {
        Map<String, String> map = new HashMap<>();
        map.put(String.format("dbus%sStreamName", getDbusServiceName()), dbusStreamName);
        map.put(String.format("dbus%sAppName", getDbusServiceName()), dbusAppName);
        return map;
    }
}
