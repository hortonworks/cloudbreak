package com.sequenceiq.freeipa.api.client;

import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGenerator;

public class FreeipaInternalCrnClient {

    private FreeIpaApiUserCrnClient client;

    private RegionAwareInternalCrnGenerator regionAwareInternalCrnGenerator;

    public FreeipaInternalCrnClient(FreeIpaApiUserCrnClient crnClient, RegionAwareInternalCrnGenerator builder) {
        client = crnClient;
        regionAwareInternalCrnGenerator = builder;
    }

    public FreeIpaApiUserCrnEndpoint withInternalCrn() {
        return client.withCrn(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString());
    }

    public FreeIpaApiUserCrnEndpoint withUserCrn(String userCrn) {
        return client.withCrn(userCrn);
    }
}
