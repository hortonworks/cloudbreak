package com.sequenceiq.sdx.client;

import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGenerator;

public class SdxInternalCrnClient {

    private SdxServiceUserCrnClient client;

    private RegionAwareInternalCrnGenerator regionAwareInternalCrnGenerator;

    public SdxInternalCrnClient(SdxServiceUserCrnClient crnClient, RegionAwareInternalCrnGenerator builder) {
        client = crnClient;
        regionAwareInternalCrnGenerator = builder;
    }

    public SdxServiceCrnEndpoints withInternalCrn() {
        return client.withCrn(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString());
    }
}
