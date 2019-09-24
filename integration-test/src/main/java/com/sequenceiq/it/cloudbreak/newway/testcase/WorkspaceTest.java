package com.sequenceiq.it.cloudbreak.newway.testcase;

import static com.sequenceiq.it.cloudbreak.newway.context.RunningParameter.key;

import javax.ws.rs.ForbiddenException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.newway.CloudbreakTest;
import com.sequenceiq.it.cloudbreak.newway.Stack;
import com.sequenceiq.it.cloudbreak.newway.StackEntity;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;

public class WorkspaceTest extends AbstractIntegrationTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(WorkspaceTest.class);

    @BeforeMethod
    public void beforeMethod(Object[] data) {
        TestContext testContext = (TestContext) data[0];

        createDefaultUser(testContext);
        createSecondUser(testContext);
        createDefaultCredential(testContext);
        createDefaultImageCatalog(testContext);

        testContext.given(StackEntity.class)
                .when(Stack.postV3())
                .await(STACK_AVAILABLE);
    }

    @AfterMethod(alwaysRun = true)
    public void tear(Object[] data) {
        TestContext testContext = (TestContext) data[0];
        testContext.cleanupTestContextEntity();
    }

    @Test(dataProvider = "testContext", enabled = false)
    public void testCreateAStackAndGetOtherUser(TestContext testContext) {
        testContext
                .given(StackEntity.class)
                .when(Stack::getByName, key("forbiddenGetByName").withWho(CloudbreakTest.SECOND_USER).withLogError(false))
                .except(ForbiddenException.class, key("forbiddenGetByName"))
                .validate();
    }
}
