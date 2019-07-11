package com.sequenceiq.environment.client;

import com.sequenceiq.cloudbreak.client.AbstractUserCrnServiceClient;
import com.sequenceiq.cloudbreak.client.ConfigKey;
import com.sequenceiq.environment.api.EnvironmentApi;

public class EnvironmentServiceCrnClient extends AbstractUserCrnServiceClient<EnvironmentServiceCrnEndpoints> {

    protected EnvironmentServiceCrnClient(String serviceAddress, ConfigKey configKey) {
        super(serviceAddress, configKey, EnvironmentApi.API_ROOT_CONTEXT);
    }

    @Override
    public EnvironmentServiceCrnEndpoints withCrn(String crn) {
        return new EnvironmentServiceCrnEndpoints(getWebTarget(), crn);
    }
}
