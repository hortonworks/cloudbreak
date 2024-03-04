package com.sequenceiq.externalizedcompute.api.client;

import com.sequenceiq.cloudbreak.client.AbstractKeyBasedServiceClient;
import com.sequenceiq.cloudbreak.client.ConfigKey;
import com.sequenceiq.externalizedcompute.api.ExternalizedComputeClusterApi;

public class ExternalizedComputeClusterApiKeyClient extends AbstractKeyBasedServiceClient<ExternalizedComputeClusterApiKeyEndpoints> {

    public ExternalizedComputeClusterApiKeyClient(String serviceAddress, ConfigKey configKey) {
        super(serviceAddress, configKey, ExternalizedComputeClusterApi.API_ROOT_CONTEXT);
    }

    @Override
    public ExternalizedComputeClusterApiKeyEndpoints withKeys(String accessKey, String secretKey) {
        return new ExternalizedComputeClusterApiKeyEndpoints(getWebTarget(), accessKey, secretKey);
    }
}