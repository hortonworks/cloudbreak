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
    public void testSimpleCreation(TestContext testContext) {
        createEnvironmentWithFreeIpa(testContext);
    }
}
