package com.sequenceiq.consumption.client;

import com.sequenceiq.cloudbreak.client.AbstractKeyBasedServiceClient;
import com.sequenceiq.cloudbreak.client.ConfigKey;
import com.sequenceiq.consumption.api.v1.ConsumptionApi;

public class ConsumptionServiceApiKeyClient extends AbstractKeyBasedServiceClient<ConsumptionServiceApiKeyEndpoints> {

    public ConsumptionServiceApiKeyClient(String serviceAddress, ConfigKey configKey) {
        super(serviceAddress, configKey, ConsumptionApi.API_ROOT_CONTEXT);
    }

    @Override
    public ConsumptionServiceApiKeyEndpoints withKeys(String accessKey, String secretKey) {
        return new ConsumptionServiceApiKeyEndpoints(getWebTarget(), accessKey, secretKey);
    }
}
