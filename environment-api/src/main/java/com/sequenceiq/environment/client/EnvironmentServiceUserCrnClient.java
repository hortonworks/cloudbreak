package com.sequenceiq.environment.client;

import com.sequenceiq.cloudbreak.client.AbstractUserCrnServiceClient;
import com.sequenceiq.cloudbreak.client.ConfigKey;

public class EnvironmentServiceUserCrnClient extends AbstractUserCrnServiceClient<EnvironmentServiceCrnEndpoints> {

    protected EnvironmentServiceUserCrnClient(String serviceAddress, ConfigKey configKey, String apiRoot) {
        super(serviceAddress, configKey, apiRoot);
    }

    @Override
    public EnvironmentServiceCrnEndpoints withCrn(String crn) {
        return new EnvironmentServiceCrnEndpoints(getWebTarget(), crn);
    }
}
