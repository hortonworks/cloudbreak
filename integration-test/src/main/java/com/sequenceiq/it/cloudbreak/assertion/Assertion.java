package com.sequenceiq.it.cloudbreak.assertion;

import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.microservice.MicroserviceClient;

public interface Assertion<T, U extends MicroserviceClient> {

    T doAssertion(TestContext testContext, T testDto, U client) throws Exception;
}
