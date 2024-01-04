package com.sequenceiq.cloudbreak.telemetry.metering;

import jakarta.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.telemetry.databus.AbstractDatabusStreamConfiguration;

@Component
public class MeteringConsumptionConfiguration extends AbstractDatabusStreamConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(MeteringConsumptionConfiguration.class);

    private final MeteringConfiguration meteringConfiguration;

    private final int numberOfWorkers;

    private final int queueSizeLimit;

    public MeteringConsumptionConfiguration(MeteringConfiguration meteringConfiguration,
            @Value("${telemetry.usage.messagebroker.workers:1}") int numberOfWorkers,
            @Value("${telemetry.usage.messagebroker.queueSizeLimit:2000}") int queueSizeLimit) {
        super(meteringConfiguration.isEnabled(), meteringConfiguration.getDbusAppName(),
                meteringConfiguration.getDbusStreamName(), meteringConfiguration.isStreamingEnabled());
        this.meteringConfiguration = meteringConfiguration;
        this.numberOfWorkers = numberOfWorkers;
        this.queueSizeLimit = queueSizeLimit;
    }

    @PostConstruct
    public void init() {
        if (isEnabled()) {
            LOGGER.info("Consumption is enabled. Configuration: {}", this);
        } else {
            LOGGER.info("Consumption is disabled. Configuration: {}", this);
        }
    }

    @Override
    public boolean isEnabled() {
        return meteringConfiguration.isEnabled() && isStreamingEnabled();
    }

    @Override
    public String getDbusServiceName() {
        return meteringConfiguration.getDbusServiceName();
    }

    @Override
    public String getDbusAppNameKey() {
        return meteringConfiguration.getDbusAppNameKey();
    }

    @Override
    public int getNumberOfWorkers() {
        return numberOfWorkers;
    }

    @Override
    public int getQueueSizeLimit() {
        return queueSizeLimit;
    }

    @Override
    public String toString() {
        return "MeteringConsumptionConfiguration{" +
                "meteringConfiguration=" + meteringConfiguration +
                ", numberOfWorkers=" + numberOfWorkers +
                ", queueSizeLimit=" + queueSizeLimit +
                "} " + super.toString();
    }
}
