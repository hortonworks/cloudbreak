package com.sequenceiq.cloudbreak.client;

import com.sequenceiq.cloudbreak.auth.crn.InternalCrnBuilder;

public class CloudbreakInternalCrnClient {

    private CloudbreakServiceUserCrnClient client;

    private InternalCrnBuilder internalCrnBuilder;

    public CloudbreakInternalCrnClient(CloudbreakServiceUserCrnClient crnClient, InternalCrnBuilder builder) {
        client = crnClient;
        internalCrnBuilder = builder;
    }

    public CloudbreakServiceCrnEndpoints withInternalCrn() {
        return client.withCrn(internalCrnBuilder.getInternalCrnForServiceAsString());
    }
}
