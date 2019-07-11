package com.sequenceiq.cloudbreak.client;

import com.sequenceiq.cloudbreak.api.CoreApi;

public class CloudbreakApiKeyClient extends AbstractKeyBasedServiceClient<CloudbreakApiKeyEndpoints> {

    public CloudbreakApiKeyClient(String serviceAddress, ConfigKey configKey) {
        super(serviceAddress, configKey, CoreApi.API_ROOT_CONTEXT);
    }

    @Override
    public CloudbreakApiKeyEndpoints withKeys(String accessKey, String secretKey) {
        return new CloudbreakApiKeyEndpoints(getWebTarget(), accessKey, secretKey);
    }
}
