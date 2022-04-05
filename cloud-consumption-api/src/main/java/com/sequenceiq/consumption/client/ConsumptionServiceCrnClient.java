package com.sequenceiq.consumption.client;

import com.sequenceiq.cloudbreak.client.AbstractUserCrnServiceClient;
import com.sequenceiq.cloudbreak.client.ConfigKey;
import com.sequenceiq.consumption.api.ConsumptionApi;

public class ConsumptionServiceCrnClient extends AbstractUserCrnServiceClient<ConsumptionServiceCrnEndpoints> {

    protected ConsumptionServiceCrnClient(String serviceAddress, ConfigKey configKey) {
        super(serviceAddress, configKey, ConsumptionApi.API_ROOT_CONTEXT);
    }

    @Override
    public ConsumptionServiceCrnEndpoints withCrn(String crn) {
        return new ConsumptionServiceCrnEndpoints(getWebTarget(), crn);
    }
}
