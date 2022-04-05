package com.sequenceiq.consumption.client;

import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;

public class ConsumptionInternalCrnClient {

    private ConsumptionServiceUserCrnClient client;

    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    public ConsumptionInternalCrnClient(ConsumptionServiceUserCrnClient crnClient,
        RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory) {
        this.client = crnClient;
        this.regionAwareInternalCrnGeneratorFactory = regionAwareInternalCrnGeneratorFactory;
    }

    public ConsumptionServiceCrnEndpoints withInternalCrn() {
        return client.withCrn(regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString());
    }
}
