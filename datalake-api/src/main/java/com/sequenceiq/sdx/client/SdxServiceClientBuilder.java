package com.sequenceiq.sdx.client;

import com.sequenceiq.cloudbreak.client.AbstractUserCrnServiceClientBuilder;
import com.sequenceiq.cloudbreak.client.ConfigKey;

public class SdxServiceClientBuilder extends AbstractUserCrnServiceClientBuilder<SdxServiceCrnClient> {

    public SdxServiceClientBuilder(String serviceAddress) {
        super(serviceAddress);
    }

    @Override
    protected SdxServiceCrnClient createUserCrnClient(String serviceAddress, ConfigKey configKey) {
        return new SdxServiceCrnClient(serviceAddress, configKey);
    }
}
