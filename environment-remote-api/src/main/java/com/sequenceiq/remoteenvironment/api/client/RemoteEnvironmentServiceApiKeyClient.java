package com.sequenceiq.remoteenvironment.api.client;

import com.sequenceiq.cloudbreak.client.AbstractKeyBasedServiceClient;
import com.sequenceiq.cloudbreak.client.ConfigKey;
import com.sequenceiq.remoteenvironment.api.RemoteEnvironmentApi;

public class RemoteEnvironmentServiceApiKeyClient extends AbstractKeyBasedServiceClient<RemoteEnvironmentServiceApiKeyEndpoints> {

    public RemoteEnvironmentServiceApiKeyClient(String serviceAddress, ConfigKey configKey) {
        super(serviceAddress, configKey, RemoteEnvironmentApi.API_ROOT_CONTEXT);
    }

    @Override
    public RemoteEnvironmentServiceApiKeyEndpoints withKeys(String accessKey, String secretKey) {
        return new RemoteEnvironmentServiceApiKeyEndpoints(getWebTarget(), accessKey, secretKey);
    }
}
