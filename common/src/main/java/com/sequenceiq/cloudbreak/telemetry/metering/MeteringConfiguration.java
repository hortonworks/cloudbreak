package com.sequenceiq.cloudbreak.telemetry.metering;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MeteringConfiguration {

    private final boolean enabled;

    private final String dbusAppName;

    private final String dbusStreamName;

    public MeteringConfiguration(@Value("${metering.enabled:false}") boolean enabled,
            @Value("${metering.dbus.app.name:}") String dbusAppName,
            @Value("${metering.dbus.stream.name:Metering}") String dbusStreamName) {
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
