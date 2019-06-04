package com.sequenceiq.it.cloudbreak.assertion;

import com.sequenceiq.it.cloudbreak.MicroserviceClient;
import com.sequenceiq.it.cloudbreak.context.TestContext;

public interface Assertion<T, U extends MicroserviceClient> {

    T doAssertion(TestContext testContext, T testDto, U client) throws Exception;
}
