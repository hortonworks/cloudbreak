package com.sequenceiq.sdx.client;

import com.sequenceiq.cloudbreak.client.AbstractUserCrnServiceClient;
import com.sequenceiq.cloudbreak.client.ConfigKey;
import com.sequenceiq.sdx.api.SdxApi;

public class SdxServiceCrnClient extends AbstractUserCrnServiceClient<SdxServiceCrnEndpoints> {

    protected SdxServiceCrnClient(String serviceAddress, ConfigKey configKey) {
        super(serviceAddress, configKey, SdxApi.API_ROOT_CONTEXT);
    }

    @Override
    public SdxServiceCrnEndpoints withCrn(String crn) {
        return new SdxServiceCrnEndpoints(getWebTarget(), crn);
    }
}
