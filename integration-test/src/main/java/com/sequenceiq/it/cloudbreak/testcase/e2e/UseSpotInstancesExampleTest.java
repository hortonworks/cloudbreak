package com.sequenceiq.it.cloudbreak.testcase.e2e;

import static org.junit.jupiter.api.Assertions.fail;

import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.testcase.AbstractMinimalTest;
import com.sequenceiq.it.cloudbreak.util.spot.UseSpotInstances;

/**
 * Minimal example test that can be run locally to test the spot retry logic quickly.
 */
public class UseSpotInstancesExampleTest extends AbstractE2ETest {

    @Override
    protected void setupTest(TestContext testContext) {
        // do nothing
    }

    @Test(dataProvider = AbstractMinimalTest.TEST_CONTEXT)
    @UseSpotInstances
    @Description(
            given = "an example spot test",
            when = "the test runs successfully",
            then = "it should not be retried"
    )
    public void success(TestContext testContext) {

    }

    @Test(dataProvider = AbstractMinimalTest.TEST_CONTEXT)
    @UseSpotInstances
    @Description(
            given = "an example spot test",
            when = "the test fails",
            then = "it should be retried"
    )
    public void failure(TestContext testContext) {
        fail("fail");
    }
}
