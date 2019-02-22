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

public class ShowBlueprintTest extends AbstractIntegrationTest {

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
            description = "When cluster does not exist the we should return with the future blueprint")
    public void testGetBlueprintWhenClusterIsNotAliveThenShouldReturnWithBlueprint(MockedTestContext testContext) {
        String clusterName = getNameGenerator().getRandomNameForResource();
        testContext
                .given(StackTestDto.class).valid()
                .withName(clusterName)
                .when(Stack.generatedBlueprint())
                .then(ShowBlueprintTest::checkFutureBlueprint)
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK,
            description = "When cluster exist the we should return with the generated blueprint")
    public void testGetBlueprintWhenClusterIsAliveThenShouldReturnWithBlueprint(MockedTestContext testContext) {
        String clusterName = getNameGenerator().getRandomNameForResource();
        testContext
                .given(StackTestDto.class).valid()
                .withName(clusterName)
                .when(Stack.postV4())
                .await(STACK_AVAILABLE)
                .when(Stack.getV4())
                .then(ShowBlueprintTest::checkGeneratedBlueprint)
                .validate();
    }

    private static StackTestDto checkFutureBlueprint(TestContext testContext, StackTestDto stackTestDto, CloudbreakClient cloudbreakClient) {
        String extendedBlueprintText = stackTestDto.getGeneratedClusterDefinition().getClusterDefinitionText();
        validateGeneratedBlueprint(extendedBlueprintText);
        return stackTestDto;
    }

    private static StackTestDto checkGeneratedBlueprint(TestContext testContext, StackTestDto stackTestDto, CloudbreakClient cloudbreakClient) {
        String extendedBlueprintText = stackTestDto.getResponse().getCluster().getAmbari().getExtendedClusterDefinitionText();
        validateGeneratedBlueprint(extendedBlueprintText);
        return stackTestDto;
    }

    private static void validateGeneratedBlueprint(String extendedBlueprintText) {
        if (Strings.isNullOrEmpty(extendedBlueprintText)) {
            throw new TestFailException("Generated Blueprint does not exist");
        } else if (!isJSONValid(extendedBlueprintText)) {
            throw new TestFailException("Generated Blueprint is not a valid json");
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
