package com.sequenceiq.it.cloudbreak.newway.testcase.mock;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.newway.Stack;
import com.sequenceiq.it.cloudbreak.newway.context.Description;
import com.sequenceiq.it.cloudbreak.newway.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.stack.StackTestDto;
import com.sequenceiq.it.cloudbreak.newway.testcase.AbstractIntegrationTest;
import com.sequenceiq.it.cloudbreak.newway.util.ShowClusterDefinitionUtil;

public class ShowClusterDefinitionTest extends AbstractIntegrationTest {

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

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(given = "stack", when = "cluster does not exist ", then = "we should return with the future cluster definition")
    public void testGetClusterDefinitionWhenClusterIsNotAliveThenShouldReturnWithClusterDefinition(MockedTestContext testContext) {
        String clusterName = getNameGenerator().getRandomNameForResource();
        testContext
                .given(StackTestDto.class)
                .valid()
                .withName(clusterName)
                .when(Stack.generatedClusterDefinition())
                .then(ShowClusterDefinitionUtil::checkFutureClusterDefinition)
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(given = "stack", when = "cluster exist ", then = "we should return with the generated cluster definition")
    public void testGetClusterDefinitionWhenClusterIsAliveThenShouldReturnWithClusterDefinition(MockedTestContext testContext) {
        String clusterName = getNameGenerator().getRandomNameForResource();
        testContext
                .given(StackTestDto.class)
                .valid()
                .withName(clusterName)
                .when(Stack.postV4())
                .await(STACK_AVAILABLE)
                .when(Stack.getV4())
                .then(ShowClusterDefinitionUtil::checkGeneratedClusterDefinition)
                .validate();
    }

}
