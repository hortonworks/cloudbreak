package com.sequenceiq.freeipa.api.client;

import com.sequenceiq.cloudbreak.client.AbstractKeyBasedServiceClient;
import com.sequenceiq.cloudbreak.client.ConfigKey;
import com.sequenceiq.freeipa.api.FreeIpaApi;

public class FreeIpaApiKeyClient extends AbstractKeyBasedServiceClient<FreeIpaApiKeyEndpoints> {

    public FreeIpaApiKeyClient(String serviceAddress, ConfigKey configKey) {
        super(serviceAddress, configKey, FreeIpaApi.API_ROOT_CONTEXT);
    }

    @Override
    public FreeIpaApiKeyEndpoints withKeys(String accessKey, String secretKey) {
        return new FreeIpaApiKeyEndpoints(getWebTarget(), accessKey, secretKey);
    }
}
