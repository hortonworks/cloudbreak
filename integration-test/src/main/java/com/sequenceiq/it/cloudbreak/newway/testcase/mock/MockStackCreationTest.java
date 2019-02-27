package com.sequenceiq.it.cloudbreak.newway.testcase.mock;

import static com.sequenceiq.it.cloudbreak.newway.context.RunningParameter.key;

import javax.ws.rs.BadRequestException;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.newway.Stack;
import com.sequenceiq.it.cloudbreak.newway.context.Description;
import com.sequenceiq.it.cloudbreak.newway.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.stack.StackTestDto;
import com.sequenceiq.it.cloudbreak.newway.testcase.AbstractIntegrationTest;

public class MockStackCreationTest extends AbstractIntegrationTest {

    @BeforeMethod
    public void beforeMethod(Object[] data) {
        MockedTestContext testContext = (MockedTestContext) data[0];
        minimalSetupForClusterCreation(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "a valid stack request",
            when = "create stack twice",
            then = "getting BadRequestException in the second time because the names are same")
    public void testAttemptToCreateTwoRegularClusterWithTheSameName(TestContext testContext) {
        String badRequest = getNameGenerator().getRandomNameForResource();
        testContext.given(StackTestDto.class)
                .when(Stack.postV4())
                .when(Stack.postV4(), key(badRequest))
                .expect(BadRequestException.class, key(badRequest))
                .validate();
    }

    @AfterMethod(alwaysRun = true)
    public void teardown(Object[] data) {
        MockedTestContext testContext = (MockedTestContext) data[0];
        testContext.cleanupTestContextEntity();
    }
}
