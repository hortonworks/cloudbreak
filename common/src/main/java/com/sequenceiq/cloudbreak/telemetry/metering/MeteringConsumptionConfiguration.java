package com.sequenceiq.cloudbreak.telemetry.metering;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.telemetry.databus.AbstractDatabusStreamConfiguration;

@Component
public class MeteringConsumptionConfiguration extends AbstractDatabusStreamConfiguration {

    private final MeteringConfiguration meteringConfiguration;

    private final boolean consumptionEnabled;

    public MeteringConsumptionConfiguration(MeteringConfiguration meteringConfiguration,
            @Value("${metering.consumption.enabled:false}") boolean consumptionEnabled) {
        super(meteringConfiguration.isEnabled(), meteringConfiguration.getDbusAppName(), meteringConfiguration.getDbusStreamName());
        this.meteringConfiguration = meteringConfiguration;
        this.consumptionEnabled = consumptionEnabled;
    }

    public boolean isConsumptionEnabled() {
        return consumptionEnabled;
    }

    @Override
    public boolean isEnabled() {
        return meteringConfiguration.isEnabled() && consumptionEnabled;
    }

    @Override
    public String getDbusServiceName() {
        return meteringConfiguration.getDbusServiceName();
    }

    @Override
    public String getDbusAppNameKey() {
        return meteringConfiguration.getDbusAppNameKey();
    }
}
