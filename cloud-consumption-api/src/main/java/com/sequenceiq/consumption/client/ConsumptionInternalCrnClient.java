package com.sequenceiq.consumption.client;

import com.sequenceiq.cloudbreak.auth.crn.InternalCrnBuilder;

public class ConsumptionInternalCrnClient {

    private ConsumptionServiceUserCrnClient client;

    private InternalCrnBuilder internalCrnBuilder;

    public ConsumptionInternalCrnClient(ConsumptionServiceUserCrnClient crnClient, InternalCrnBuilder builder) {
        client = crnClient;
        internalCrnBuilder = builder;
    }

    public ConsumptionServiceCrnEndpoints withInternalCrn() {
        return client.withCrn(internalCrnBuilder.getInternalCrnForServiceAsString());
    }
}
