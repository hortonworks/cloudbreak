package com.sequenceiq.it.cloudbreak.newway.testcase.mock;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.Stack;
import com.sequenceiq.it.cloudbreak.newway.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.stack.StackTestDto;
import com.sequenceiq.it.cloudbreak.newway.testcase.AbstractIntegrationTest;

public class ShowStackCliRequestTest extends AbstractIntegrationTest {

    @BeforeMethod
    public void beforeMethod(Object[] data) {
        minimalSetupForClusterCreation((MockedTestContext) data[0]);
    }

    protected void minimalSetupForClusterCreation(TestContext testContext) {
        createDefaultUser(testContext);
        createDefaultCredential(testContext);
        createDefaultEnvironment(testContext);
        createDefaultImageCatalog(testContext);
        initializeDefaultClusterDefinitions(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK,
            description = "When cluster exist the we should return with the cli json")
    public void testGetBlueprintWhenClusterIsAliveThenShouldReturnWithBlueprint(MockedTestContext testContext) {
        String clusterName = getNameGenerator().getRandomNameForResource();
        testContext
                .given(StackTestDto.class).valid()
                .withName(clusterName)
                .when(Stack.postV4())
                .await(STACK_AVAILABLE)
                .when(Stack.getCli())
                .then(ShowStackCliRequestTest::checkCliSkeleton)
                .validate();
    }

    private static StackTestDto checkCliSkeleton(TestContext testContext, StackTestDto stackTestDto, CloudbreakClient cloudbreakClient) {
        if (stackTestDto.getRequest() == null) {
            throw new TestFailException("Generated cli skeleton does not exist");
        }
        return stackTestDto;
    }
}
