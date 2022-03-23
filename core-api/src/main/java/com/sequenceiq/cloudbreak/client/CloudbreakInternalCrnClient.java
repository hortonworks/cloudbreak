package com.sequenceiq.cloudbreak.client;

import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGenerator;

public class CloudbreakInternalCrnClient {

    private CloudbreakServiceUserCrnClient client;

    private RegionAwareInternalCrnGenerator regionAwareInternalCrnGenerator;

    public CloudbreakInternalCrnClient(CloudbreakServiceUserCrnClient crnClient, RegionAwareInternalCrnGenerator builder) {
        client = crnClient;
        regionAwareInternalCrnGenerator = builder;
    }

    public CloudbreakServiceCrnEndpoints withInternalCrn() {
        return client.withCrn(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString());
    }
}
