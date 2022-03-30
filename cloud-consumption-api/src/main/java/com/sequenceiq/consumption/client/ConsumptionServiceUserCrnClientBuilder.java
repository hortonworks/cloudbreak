package com.sequenceiq.consumption.client;

import com.sequenceiq.cloudbreak.client.AbstractUserCrnServiceClientBuilder;
import com.sequenceiq.cloudbreak.client.ConfigKey;
import com.sequenceiq.consumption.api.ConsumptionApi;

public class ConsumptionServiceUserCrnClientBuilder extends AbstractUserCrnServiceClientBuilder<ConsumptionServiceUserCrnClient> {

    public ConsumptionServiceUserCrnClientBuilder(String serviceAddress) {
        super(serviceAddress);
    }

    @Override
    protected ConsumptionServiceUserCrnClient createUserCrnClient(String serviceAddress, ConfigKey configKey) {
        return new ConsumptionServiceUserCrnClient(serviceAddress, configKey, ConsumptionApi.API_ROOT_CONTEXT);
    }
}
