package com.sequenceiq.freeipa.api.client;

import com.sequenceiq.cloudbreak.client.ConfigKey;
import com.sequenceiq.cloudbreak.client.AbstractUserCrnServiceClient;

public class FreeIpaApiUserCrnClientFactory extends AbstractUserCrnServiceClient {
    public FreeIpaApiUserCrnClientFactory(String serviceAddress, ConfigKey configKey, String apiRoot) {
        super(serviceAddress, configKey, apiRoot);
    }

    @Override
    public FreeIpaApiUserCrnClient withCrn(String crn) {
        return new FreeIpaApiUserCrnClient(getWebTarget(), crn);
    }
}
