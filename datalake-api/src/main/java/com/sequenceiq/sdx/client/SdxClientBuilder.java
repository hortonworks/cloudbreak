package com.sequenceiq.sdx.client;

import com.sequenceiq.cloudbreak.client.AbstractUserCrnServiceClientBuilder;
import com.sequenceiq.cloudbreak.client.ConfigKey;

public class SdxClientBuilder extends AbstractUserCrnServiceClientBuilder {
    public SdxClientBuilder(String serviceAddress) {
        super(serviceAddress);
    }

    @Override
    protected SdxServiceClient createUserCrnClient(String serviceAddress, ConfigKey configKey) {
        return new SdxServiceClient(serviceAddress, configKey);
    }
}
