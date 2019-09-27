package com.sequenceiq.redbeams.client;

import com.sequenceiq.cloudbreak.client.AbstractKeyBasedServiceClient;
import com.sequenceiq.cloudbreak.client.ConfigKey;
import com.sequenceiq.redbeams.api.RedbeamsApi;

public class RedbeamsApiKeyClient extends AbstractKeyBasedServiceClient<RedbeamsApiKeyEndpoints> {

    public RedbeamsApiKeyClient(String serviceAddress, ConfigKey configKey) {
        super(serviceAddress, configKey, RedbeamsApi.API_ROOT_CONTEXT);
    }

    @Override
    public RedbeamsApiKeyEndpoints withKeys(String accessKey, String secretKey) {
        return new RedbeamsApiKeyEndpoints(getWebTarget(), accessKey, secretKey);
    }
}
