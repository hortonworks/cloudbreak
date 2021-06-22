package com.sequenceiq.environment.client;

import com.sequenceiq.cloudbreak.auth.crn.InternalCrnBuilder;

public class EnvironmentInternalCrnClient {

    private EnvironmentServiceUserCrnClient client;

    private InternalCrnBuilder internalCrnBuilder;

    public EnvironmentInternalCrnClient(EnvironmentServiceUserCrnClient crnClient, InternalCrnBuilder builder) {
        client = crnClient;
        internalCrnBuilder = builder;
    }

    public EnvironmentServiceCrnEndpoints withInternalCrn() {
        return client.withCrn(internalCrnBuilder.getInternalCrnForServiceAsString());
    }
}
