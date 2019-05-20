package com.sequenceiq.it.cloudbreak.assertion;

import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.context.TestContext;

public interface Assertion<T> {

    T doAssertion(TestContext testContext, T testDto, CloudbreakClient cloudbreakClient) throws Exception;
}
