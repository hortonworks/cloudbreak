package com.sequenceiq.environment.client;

import com.sequenceiq.cloudbreak.client.AbstractUserCrnServiceClient;
import com.sequenceiq.cloudbreak.client.ConfigKey;
import com.sequenceiq.environment.api.EnvironmentApi;

public class EnvironmentServiceClient extends AbstractUserCrnServiceClient<EnvironmentServiceEndpoints> {

    protected EnvironmentServiceClient(String serviceAddress, ConfigKey configKey) {
        super(serviceAddress, configKey, EnvironmentApi.API_ROOT_CONTEXT);
    }

    @Override
    public EnvironmentServiceEndpoints withCrn(String crn) {
        return new EnvironmentServiceEndpoints(getWebTarget(), crn);
    }
}
