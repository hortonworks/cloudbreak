package com.sequenceiq.it.cloudbreak.newway.assertion;

import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;

public interface AssertionV2<T> {

    T doAssertion(TestContext testContext, T entity, CloudbreakClient cloudbreakClient) throws Exception;
}
