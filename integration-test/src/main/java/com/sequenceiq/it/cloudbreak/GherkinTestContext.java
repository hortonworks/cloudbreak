package com.sequenceiq.it.cloudbreak;

import com.sequenceiq.it.IntegrationTestContext;

public class GherkinTestContext extends GherkinTest {
    private final GherkinTest test;

    public GherkinTestContext(GherkinTest test) {
        this.test = test;
    }

    public IntegrationTestContext getIntegrationTestContext() {
        return test.getItContext();
    }
}
