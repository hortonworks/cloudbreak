package com.sequenceiq.environment.client;

import com.sequenceiq.cloudbreak.client.AbstractKeyBasedServiceClient;
import com.sequenceiq.cloudbreak.client.ConfigKey;
import com.sequenceiq.environment.api.EnvironmentApi;

public class EnvironmentServiceApiKeyClient extends AbstractKeyBasedServiceClient<EnvironmentServiceApiKeyEndpoints> {

    public EnvironmentServiceApiKeyClient(String serviceAddress, ConfigKey configKey) {
        super(serviceAddress, configKey, EnvironmentApi.API_ROOT_CONTEXT);
    }

    @Override
    public EnvironmentServiceApiKeyEndpoints withKeys(String accessKey, String secretKey) {
        return new EnvironmentServiceApiKeyEndpoints(getWebTarget(), accessKey, secretKey);
    }
}
