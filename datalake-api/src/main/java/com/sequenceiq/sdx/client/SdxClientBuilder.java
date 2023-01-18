package com.sequenceiq.sdx.client;

import com.sequenceiq.cloudbreak.client.AbstractUserCrnServiceClientBuilder;
import com.sequenceiq.cloudbreak.client.ConfigKey;

public class SdxClientBuilder extends AbstractUserCrnServiceClientBuilder<SdxServiceCrnClient> {

    public SdxClientBuilder(String serviceAddress) {
        super(serviceAddress);
    }

    @Override
    protected SdxServiceCrnClient createUserCrnClient(String serviceAddress, ConfigKey configKey) {
        return new SdxServiceCrnClient(serviceAddress, configKey);
    }
}
