package com.sequenceiq.cloudbreak.telemetry.metering;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import com.sequenceiq.cloudbreak.telemetry.databus.AbstractDatabusStreamConfiguration;

@Configuration
public class MeteringConfiguration extends AbstractDatabusStreamConfiguration {

    public MeteringConfiguration(@Value("${metering.enabled:false}") boolean enabled,
            @Value("${metering.dbus.app.name:}") String dbusAppName,
            @Value("${metering.dbus.stream.name:Metering}") String dbusStreamName,
            @Value("${metering.consumption.enabled:false}") boolean streamingEnabled) {
        super(enabled, dbusAppName, dbusStreamName, streamingEnabled);
    }

    @Override
    public String getDbusServiceName() {
        return "Metering";
    }

    @Override
    public String getDbusAppNameKey() {
        return "@metering-app";
    }
}
