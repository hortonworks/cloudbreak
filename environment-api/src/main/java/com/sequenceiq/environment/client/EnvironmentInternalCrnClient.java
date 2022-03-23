package com.sequenceiq.environment.client;

import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGenerator;

public class EnvironmentInternalCrnClient {

    private EnvironmentServiceUserCrnClient client;

    private RegionAwareInternalCrnGenerator regionAwareInternalCrnGenerator;

    public EnvironmentInternalCrnClient(EnvironmentServiceUserCrnClient crnClient, RegionAwareInternalCrnGenerator builder) {
        client = crnClient;
        regionAwareInternalCrnGenerator = builder;
    }

    public EnvironmentServiceCrnEndpoints withInternalCrn() {
        return client.withCrn(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString());
    }
}
