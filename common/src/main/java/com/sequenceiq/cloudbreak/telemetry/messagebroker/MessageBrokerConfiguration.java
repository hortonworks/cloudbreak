package com.sequenceiq.cloudbreak.telemetry.messagebroker;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.telemetry.databus.AbstractDatabusStreamConfiguration;

@Component
public class MessageBrokerConfiguration extends AbstractDatabusStreamConfiguration {

    private final String origin;

    private final String processor;

    private final int numberOfWorkers;

    private final int queueSizeLimit;

    public MessageBrokerConfiguration(@Value("${telemetry.usage.messagebroker.enabled}") boolean enabled,
            @Value("${telemetry.usage.messagebroker.dbus-app-name}") String dbusAppName,
            @Value("${telemetry.usage.messagebroker.dbus-stream-name}") String dbusStreamName,
            @Value("${telemetry.usage.messagebroker.streaming-enabled}") boolean streamingEnabled,
            @Value("${telemetry.usage.messagebroker.headers.origin}") String origin,
            @Value("${telemetry.usage.messagebroker.headers.processor}") String processor,
            @Value("${telemetry.usage.messagebroker.workers:1}") int numberOfWorkers,
            @Value("${telemetry.usage.messagebroker.queueSizeLimit:2000}") int queueSizeLimit) {
        super(enabled, dbusAppName, dbusStreamName, streamingEnabled);
        this.origin = origin;
        this.processor = processor;
        this.numberOfWorkers = numberOfWorkers;
        this.queueSizeLimit = queueSizeLimit;
    }

    @Override
    public String getDbusServiceName() {
        return "MessageBroker";
    }

    @Override
    public String getDbusAppNameKey() {
        return "usage-events-app";
    }

    @Override
    public int getNumberOfWorkers() {
        return numberOfWorkers;
    }

    @Override
    public int getQueueSizeLimit() {
        return queueSizeLimit;
    }

    public String getOrigin() {
        return origin;
    }

    public String getProcessor() {
        return processor;
    }
}
