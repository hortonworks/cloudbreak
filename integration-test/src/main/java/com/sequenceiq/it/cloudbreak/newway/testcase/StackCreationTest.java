package com.sequenceiq.it.cloudbreak.newway.testcase;

import static com.sequenceiq.it.cloudbreak.newway.context.RunningParameter.key;

import javax.ws.rs.BadRequestException;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.it.cloudbreak.newway.Stack;
import com.sequenceiq.it.cloudbreak.newway.StackEntity;
import com.sequenceiq.it.cloudbreak.newway.action.StackPostAction;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;

public class StackCreationTest extends AbstractIntegrationTest {

    @BeforeMethod
    public void beforeMethod(Object[] data) {
        TestContext testContext = (TestContext) data[0];
        minimalSetupForClusterCreation(testContext);
    }

    @Test(dataProvider = "testContext")
    public void testCreateNewRegularCluster(TestContext testContext) {
        testContext.given(StackEntity.class)
                .when(new StackPostAction())
                .await(Status.AVAILABLE)
                .validate();
    }

    @Test(dataProvider = "testContext")
    public void testAttemptToCreateTwoRegularClusterWithTheSameName(TestContext testContext) {
        testContext.given(StackEntity.class)
                .when(Stack.postV2())
                .when(Stack.postV2(), key("badRequest"))
                .except(BadRequestException.class, key("badRequest"))
                .validate();
    }

    @AfterMethod(alwaysRun = true)
    public void tear(Object[] data) {
        TestContext testContext = (TestContext) data[0];
        testContext.cleanupTestContextEntity();
    }
}
