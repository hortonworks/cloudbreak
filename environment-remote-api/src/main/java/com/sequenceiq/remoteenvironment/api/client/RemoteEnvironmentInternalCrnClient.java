package com.sequenceiq.remoteenvironment.api.client;

import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGenerator;
import com.sequenceiq.cloudbreak.client.AbstractUserCrnServiceClient;
import com.sequenceiq.cloudbreak.client.ConfigKey;

public class RemoteEnvironmentInternalCrnClient extends AbstractUserCrnServiceClient<RemoteEnvironmentCrnEndpoint> {

    private RegionAwareInternalCrnGenerator regionAwareInternalCrnGenerator;

    public RemoteEnvironmentInternalCrnClient(String serviceAddress, ConfigKey configKey, String apiRoot, RegionAwareInternalCrnGenerator builder) {
        super(serviceAddress, configKey, apiRoot);
        regionAwareInternalCrnGenerator = builder;
    }

    public RemoteEnvironmentCrnEndpoint withInternalCrn() {
        return new RemoteEnvironmentCrnEndpoint(getWebTarget(), regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString());
    }

    public RemoteEnvironmentCrnEndpoint withCrn(String userCrn) {
        return new RemoteEnvironmentCrnEndpoint(getWebTarget(), userCrn);
    }
}