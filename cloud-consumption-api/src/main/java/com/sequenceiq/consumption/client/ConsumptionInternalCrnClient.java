package com.sequenceiq.consumption.client;

import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGenerator;

public class ConsumptionInternalCrnClient {

    private final ConsumptionServiceUserCrnClient client;

    private final RegionAwareInternalCrnGenerator regionAwareInternalCrnGenerator;

    public ConsumptionInternalCrnClient(ConsumptionServiceUserCrnClient crnClient,
        RegionAwareInternalCrnGenerator regionAwareInternalCrnGenerator) {
        this.client = crnClient;
        this.regionAwareInternalCrnGenerator = regionAwareInternalCrnGenerator;
    }

    public ConsumptionServiceCrnEndpoints withInternalCrn() {
        return client.withCrn(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString());
    }

}
