package com.sequenceiq.sdx.client;

import com.sequenceiq.cloudbreak.client.AbstractUserCrnServiceClient;
import com.sequenceiq.cloudbreak.client.ConfigKey;
import com.sequenceiq.sdx.api.SdxApi;

public class SdxServiceClient extends AbstractUserCrnServiceClient<SdxServiceEndpoints> {

    protected SdxServiceClient(String serviceAddress, ConfigKey configKey) {
        super(serviceAddress, configKey, SdxApi.API_ROOT_CONTEXT);
    }

    @Override
    public SdxServiceEndpoints withCrn(String crn) {
        return null;
    }
}
