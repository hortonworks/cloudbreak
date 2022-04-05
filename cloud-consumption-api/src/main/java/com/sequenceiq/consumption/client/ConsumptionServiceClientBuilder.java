package com.sequenceiq.consumption.client;

import com.sequenceiq.cloudbreak.client.AbstractUserCrnServiceClientBuilder;
import com.sequenceiq.cloudbreak.client.ConfigKey;

public class ConsumptionServiceClientBuilder extends AbstractUserCrnServiceClientBuilder<ConsumptionServiceCrnClient> {

    public ConsumptionServiceClientBuilder(String serviceAddress) {
        super(serviceAddress);
    }

    @Override
    protected ConsumptionServiceCrnClient createUserCrnClient(String serviceAddress, ConfigKey configKey) {
        return new ConsumptionServiceCrnClient(serviceAddress, configKey);
    }
}
