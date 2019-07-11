package com.sequenceiq.sdx.client;

import com.sequenceiq.cloudbreak.client.AbstractKeyBasedServiceClient;
import com.sequenceiq.cloudbreak.client.ConfigKey;
import com.sequenceiq.sdx.api.SdxApi;

public class SdxServiceApiKeyClient extends AbstractKeyBasedServiceClient<SdxServiceApiKeyEndpoints> {

    public SdxServiceApiKeyClient(String serviceAddress, ConfigKey configKey) {
        super(serviceAddress, configKey, SdxApi.API_ROOT_CONTEXT);
    }

    @Override
    public SdxServiceApiKeyEndpoints withKeys(String accessKey, String secretKey) {
        return new SdxServiceApiKeyEndpoints(getWebTarget(), accessKey, secretKey);
    }
}
