package com.sequenceiq.environment.client;

import com.sequenceiq.cloudbreak.client.AbstractUserCrnServiceClientBuilder;
import com.sequenceiq.cloudbreak.client.ConfigKey;
import com.sequenceiq.environment.api.EnvironmentApi;

public class EnvironmentServiceUserCrnClientBuilder extends AbstractUserCrnServiceClientBuilder<EnvironmentServiceUserCrnClient> {

    public EnvironmentServiceUserCrnClientBuilder(String serviceAddress) {
        super(serviceAddress);
    }

    @Override
    protected EnvironmentServiceUserCrnClient createUserCrnClient(String serviceAddress, ConfigKey configKey) {
        return new EnvironmentServiceUserCrnClient(serviceAddress, configKey, EnvironmentApi.API_ROOT_CONTEXT);
    }
}
