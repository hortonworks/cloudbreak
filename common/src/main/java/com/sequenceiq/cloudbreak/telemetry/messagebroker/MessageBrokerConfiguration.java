package com.sequenceiq.cloudbreak.telemetry.messagebroker;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.telemetry.databus.AbstractDatabusStreamConfiguration;

@Component
public class MessageBrokerConfiguration extends AbstractDatabusStreamConfiguration {

    private final String origin;

    private final String processor;

    public MessageBrokerConfiguration(@Value("${telemetry.usage.messagebroker.enabled}") boolean enabled,
            @Value("${telemetry.usage.messagebroker.dbus-app-name}") String dbusAppName,
            @Value("${telemetry.usage.messagebroker.dbus-stream-name}") String dbusStreamName,
            @Value("${telemetry.usage.messagebroker.headers.origin}") String origin,
            @Value("${telemetry.usage.messagebroker.headers.processor}") String processor) {
        super(enabled, dbusAppName, dbusStreamName);
        this.origin = origin;
        this.processor = processor;
    }

    @Override
    public String getDbusServiceName() {
        return "MessageBroker";
    }

    @Override
    public String getDbusAppNameKey() {
        return "usage-events-app";
    }

    public String getOrigin() {
        return origin;
    }

    public String getProcessor() {
        return processor;
    }
}
