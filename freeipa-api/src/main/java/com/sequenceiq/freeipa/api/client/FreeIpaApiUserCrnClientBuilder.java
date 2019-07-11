package com.sequenceiq.freeipa.api.client;

import com.sequenceiq.cloudbreak.client.AbstractUserCrnServiceClientBuilder;
import com.sequenceiq.cloudbreak.client.ConfigKey;
import com.sequenceiq.freeipa.api.FreeIpaApi;

public class FreeIpaApiUserCrnClientBuilder extends AbstractUserCrnServiceClientBuilder<FreeIpaApiUserCrnClient> {
    public FreeIpaApiUserCrnClientBuilder(String cloudbreakAddress) {
        super(cloudbreakAddress);
    }

    @Override
    protected FreeIpaApiUserCrnClient createUserCrnClient(String serviceAddress, ConfigKey configKey) {
        return new FreeIpaApiUserCrnClient(serviceAddress, configKey, FreeIpaApi.API_ROOT_CONTEXT);
    }
}
