package com.sequenceiq.environment.client;

import com.sequenceiq.cloudbreak.client.AbstractUserCrnServiceClientBuilder;
import com.sequenceiq.cloudbreak.client.ConfigKey;

public class EnvironmentServiceClientBuilder extends AbstractUserCrnServiceClientBuilder<EnvironmentServiceCrnClient> {

    public EnvironmentServiceClientBuilder(String serviceAddress) {
        super(serviceAddress);
    }

    @Override
    protected EnvironmentServiceCrnClient createUserCrnClient(String serviceAddress, ConfigKey configKey) {
        return new EnvironmentServiceCrnClient(serviceAddress, configKey);
    }
}
