package com.sequenceiq.freeipa.api.client;

import com.sequenceiq.cloudbreak.client.AbstractUserCrnServiceClient;
import com.sequenceiq.cloudbreak.client.ConfigKey;

public class FreeIpaApiUserCrnClientFactory extends AbstractUserCrnServiceClient<FreeIpaApiUserCrnClient> {
    public FreeIpaApiUserCrnClientFactory(String serviceAddress, ConfigKey configKey, String apiRoot) {
        super(serviceAddress, configKey, apiRoot);
    }

    @Override
    public FreeIpaApiUserCrnClient withCrn(String crn) {
        return new FreeIpaApiUserCrnClient(getWebTarget(), crn);
    }
}
