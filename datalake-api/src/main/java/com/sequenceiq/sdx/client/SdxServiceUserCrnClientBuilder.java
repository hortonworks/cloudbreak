package com.sequenceiq.sdx.client;

import com.sequenceiq.cloudbreak.client.AbstractUserCrnServiceClientBuilder;
import com.sequenceiq.cloudbreak.client.ConfigKey;
import com.sequenceiq.sdx.api.SdxApi;

public class SdxServiceUserCrnClientBuilder extends AbstractUserCrnServiceClientBuilder<SdxServiceUserCrnClient> {

    public SdxServiceUserCrnClientBuilder(String serviceAddress) {
        super(serviceAddress);
    }

    @Override
    protected SdxServiceUserCrnClient createUserCrnClient(String serviceAddress, ConfigKey configKey) {
        return new SdxServiceUserCrnClient(serviceAddress, configKey, SdxApi.API_ROOT_CONTEXT);
    }
}
