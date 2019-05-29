package com.sequenceiq.freeipa.api.client;

import com.sequenceiq.cloudbreak.client.ConfigKey;
import com.sequenceiq.cloudbreak.client.AbstractUserCrnServiceClient;

public class FreeIpaApiUserCrnClient extends AbstractUserCrnServiceClient<FreeIpaApiUserCrnEndpoint> {
    public FreeIpaApiUserCrnClient(String serviceAddress, ConfigKey configKey, String apiRoot) {
        super(serviceAddress, configKey, apiRoot);
    }

    @Override
    public FreeIpaApiUserCrnEndpoint withCrn(String crn) {
        return new FreeIpaApiUserCrnEndpoint(getWebTarget(), crn);
    }
}
