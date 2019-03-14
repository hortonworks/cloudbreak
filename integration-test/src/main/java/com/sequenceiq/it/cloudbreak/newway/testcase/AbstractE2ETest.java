package com.sequenceiq.it.cloudbreak.newway.testcase;

import com.sequenceiq.it.cloudbreak.newway.context.TestContext;

public abstract class AbstractE2ETest extends AbstractIntegrationTest {

    @Override
    protected void minimalSetupForClusterCreation(TestContext testContext) {
        createDefaultUser(testContext);
        createDefaultCredential(testContext);
        createDefaultEnvironment(testContext);
        initializeDefaultClusterDefinitions(testContext);
    }
}
