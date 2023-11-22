package com.sequenceiq.sdx.client;

import com.sequenceiq.cloudbreak.client.AbstractUserCrnServiceClient;
import com.sequenceiq.cloudbreak.client.ConfigKey;

public class SdxServiceUserCrnClient extends AbstractUserCrnServiceClient<SdxServiceCrnEndpoints> {

    protected SdxServiceUserCrnClient(String serviceAddress, ConfigKey configKey, String apiRoot) {
        super(serviceAddress, configKey, apiRoot);
    }

    @Override
    public SdxServiceCrnEndpoints withCrn(String crn) {
        return new SdxServiceCrnEndpoints(getWebTarget(), crn);
    }
}
