package com.sequenceiq.it.cloudbreak.assertion;

import com.sequenceiq.it.cloudbreak.MicroserviceClient;
import com.sequenceiq.it.cloudbreak.actor.CloudbreakUser;
import com.sequenceiq.it.cloudbreak.context.TestContext;

public class BaseMicroserviceClientDependentAssertion {

    protected BaseMicroserviceClientDependentAssertion() {
    }

    @SuppressWarnings("unchecked")
    protected static <T extends MicroserviceClient> T getClient(TestContext testContext, CloudbreakUser actor,
            Class<? extends MicroserviceClient> serviceClass) {

        return (T) testContext.getClients()
                .get(actor.getAccessKey())
                .get(serviceClass);
    }
}
