package com.sequenceiq.freeipa.api.client;

import com.sequenceiq.cloudbreak.auth.crn.InternalCrnBuilder;

public class FreeipaInternalCrnClient {

    private FreeIpaApiUserCrnClient client;

    private InternalCrnBuilder internalCrnBuilder;

    public FreeipaInternalCrnClient(FreeIpaApiUserCrnClient crnClient, InternalCrnBuilder builder) {
        client = crnClient;
        internalCrnBuilder = builder;
    }

    public FreeIpaApiUserCrnEndpoint withInternalCrn() {
        return client.withCrn(internalCrnBuilder.getInternalCrnForServiceAsString());
    }

    public FreeIpaApiUserCrnEndpoint withUserCrn(String userCrn) {
        return client.withCrn(userCrn);
    }
}
