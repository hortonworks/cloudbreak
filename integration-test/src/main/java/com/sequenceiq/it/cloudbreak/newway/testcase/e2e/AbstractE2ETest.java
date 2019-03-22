package com.sequenceiq.it.cloudbreak.newway.testcase.e2e;

import javax.inject.Inject;

import com.sequenceiq.it.cloudbreak.newway.TestParameter;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.testcase.AbstractIntegrationTest;

public abstract class AbstractE2ETest extends AbstractIntegrationTest {

    @Inject
    private TestParameter testParameter;

    protected TestParameter getTestParameter() {
        return testParameter;
    }

    @Override
    protected void minimalSetupForClusterCreation(TestContext testContext) {
        createDefaultUser(testContext);
        createDefaultCredential(testContext);
        createDefaultEnvironment(testContext);
        initializeDefaultClusterDefinitions(testContext);
    }
}
