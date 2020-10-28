package com.sequenceiq.cloudbreak.telemetry.databus;

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
}
