package com.sequenceiq.consumption.client;

import com.sequenceiq.cloudbreak.client.AbstractUserCrnServiceClient;
import com.sequenceiq.cloudbreak.client.ConfigKey;

public class ConsumptionServiceUserCrnClient extends AbstractUserCrnServiceClient<ConsumptionServiceCrnEndpoints> {

    protected ConsumptionServiceUserCrnClient(String serviceAddress, ConfigKey configKey, String apiRoot) {
        super(serviceAddress, configKey, apiRoot);
    }

    @Override
    public ConsumptionServiceCrnEndpoints withCrn(String crn) {
        return new ConsumptionServiceCrnEndpoints(getWebTarget(), crn);
    }
}
