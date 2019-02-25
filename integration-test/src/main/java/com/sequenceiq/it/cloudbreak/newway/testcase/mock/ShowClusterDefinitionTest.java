package com.sequenceiq.it.cloudbreak.newway.testcase.mock;

import java.io.IOException;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.Stack;
import com.sequenceiq.it.cloudbreak.newway.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.stack.StackTestDto;
import com.sequenceiq.it.cloudbreak.newway.testcase.AbstractIntegrationTest;

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

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK,
            description = "When cluster does not exist the we should return with the future cluster definition")
    public void testGetClusterDefinitionWhenClusterIsNotAliveThenShouldReturnWithClusterDefinition(MockedTestContext testContext) {
        String clusterName = getNameGenerator().getRandomNameForResource();
        testContext
                .given(StackTestDto.class).valid()
                .withName(clusterName)
                .when(Stack.generatedClusterDefinition())
                .then(ShowClusterDefinitionTest::checkFutureClusterDefinition)
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK,
            description = "When cluster exist the we should return with the generated cluster definition")
    public void testGetClusterDefinitionWhenClusterIsAliveThenShouldReturnWithClusterDefinition(MockedTestContext testContext) {
        String clusterName = getNameGenerator().getRandomNameForResource();
        testContext
                .given(StackTestDto.class).valid()
                .withName(clusterName)
                .when(Stack.postV4())
                .await(STACK_AVAILABLE)
                .when(Stack.getV4())
                .then(ShowClusterDefinitionTest::checkGeneratedClusterDefinition)
                .validate();
    }

    private static StackTestDto checkFutureClusterDefinition(TestContext testContext, StackTestDto stackTestDto, CloudbreakClient cloudbreakClient) {
        String extendedClusterDefinitionText = stackTestDto.getGeneratedClusterDefinition().getClusterDefinitionText();
        validateGeneratedClusterDefinition(extendedClusterDefinitionText);
        return stackTestDto;
    }

    private static StackTestDto checkGeneratedClusterDefinition(TestContext testContext, StackTestDto stackTestDto, CloudbreakClient cloudbreakClient) {
        String extendedClusterDefinitionText = stackTestDto.getResponse().getCluster().getAmbari().getExtendedClusterDefinitionText();
        validateGeneratedClusterDefinition(extendedClusterDefinitionText);
        return stackTestDto;
    }

    private static void validateGeneratedClusterDefinition(String extendedClusterDefinitionText) {
        if (Strings.isNullOrEmpty(extendedClusterDefinitionText)) {
            throw new TestFailException("Generated Cluster Definition does not exist");
        } else if (!isJSONValid(extendedClusterDefinitionText)) {
            throw new TestFailException("Generated Cluster Definition is not a valid json");
        }
    }

    public static boolean isJSONValid(String jsonInString) {
        try {
            final ObjectMapper mapper = new ObjectMapper();
            mapper.readTree(jsonInString);
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
