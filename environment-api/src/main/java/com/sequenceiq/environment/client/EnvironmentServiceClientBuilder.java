package com.sequenceiq.environment.client;

import com.sequenceiq.cloudbreak.client.AbstractUserCrnServiceClientBuilder;
import com.sequenceiq.cloudbreak.client.ConfigKey;

public class EnvironmentServiceClientBuilder extends AbstractUserCrnServiceClientBuilder {

    public EnvironmentServiceClientBuilder(String serviceAddress) {
        super(serviceAddress);
    }

    @Override
    protected EnvironmentServiceClient createUserCrnClient(String serviceAddress, ConfigKey configKey) {
        return new EnvironmentServiceClient(serviceAddress, configKey);
    }
}
