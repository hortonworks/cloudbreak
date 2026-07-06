package com.sequenceiq.it.cloudbreak.testcase.e2e.pullrequest;

import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.testcase.e2e.AbstractE2ETest;

public class PullRequestE2ETest extends AbstractE2ETest {

    @Override
    protected void setupTest(TestContext testContext) {
        createDefaultUser(testContext);
        createDefaultCredential(testContext);
        initializeDefaultBlueprints(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT)
    @Description(
            given   = "Deploy Freeipa",
            when    = "Pull Request check happens",
            then    = "all freeipa and environment must came up"
    )
    public void testSimpleEnvironmentWithFreeipa(TestContext testContext) {
        createEnvironment(testContext, Boolean.TRUE, 1);
    }

    @Test(dataProvider = TEST_CONTEXT)
    @Description(
            given   = "Deploy Datalake",
            when    = "Pull Request check happens",
            then    = "environment and datalake must came up"
    )
    public void testSimpleDatalake(TestContext testContext) {
        createDefaultDatalake(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT)
    @Description(
            given   = "Deploy Distrox",
            when    = "Pull Request check happens",
            then    = "environment datalake and datahub must came up"
    )
    public void testSimpleDatahub(TestContext testContext) {
        createDefaultDatahub(testContext);
    }
}
