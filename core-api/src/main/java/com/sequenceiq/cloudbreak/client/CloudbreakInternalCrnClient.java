package com.sequenceiq.cloudbreak.client;

import com.sequenceiq.cloudbreak.auth.altus.InternalCrnBuilder;
import com.sequenceiq.cloudbreak.client.CloudbreakUserCrnClient.CloudbreakEndpoint;

public class CloudbreakInternalCrnClient {

    private CloudbreakUserCrnClient client;

    private InternalCrnBuilder internalCrnBuilder;

    public CloudbreakInternalCrnClient(CloudbreakUserCrnClient crnClient, InternalCrnBuilder builder) {
        client = crnClient;
        internalCrnBuilder = builder;
    }

    public CloudbreakEndpoint withInternalCrn() {
        return client.withCrn(internalCrnBuilder.getInternalCrnForServiceAsString());
    }
}
