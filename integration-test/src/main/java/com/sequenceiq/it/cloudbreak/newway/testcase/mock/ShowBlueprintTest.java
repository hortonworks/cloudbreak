package com.sequenceiq.it.cloudbreak.newway.testcase.mock;

import javax.inject.Inject;

import org.testng.annotations.Test;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.util.JsonUtil;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.newway.client.StackTestClient;
import com.sequenceiq.it.cloudbreak.newway.context.Description;
import com.sequenceiq.it.cloudbreak.newway.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.newway.dto.stack.StackTestDto;
import com.sequenceiq.it.cloudbreak.newway.testcase.AbstractIntegrationTest;
import com.sequenceiq.it.cloudbreak.newway.util.ShowBlueprintUtil;

public class ShowBlueprintTest extends AbstractIntegrationTest {

    @Inject
    private StackTestClient stackTestClient;

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(given = "stack", when = "cluster does not exist ", then = "we should return with the future blueprint")
    public void testGetBlueprintWhenClusterIsNotAliveThenShouldReturnWithBlueprint(MockedTestContext testContext) {
        String clusterName = resourcePropertyProvider().getName();
        testContext
                .given(StackTestDto.class)
                .valid()
                .withName(clusterName)
                .when(stackTestClient.blueprintRequestV4())
                .then(ShowBlueprintUtil::checkFutureBlueprint)
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(given = "stack", when = "cluster exist ", then = "we should return with the generated blueprint")
    public void testGetBlueprintWhenClusterIsAliveThenShouldReturnWithBlueprint(MockedTestContext testContext) {
        String clusterName = resourcePropertyProvider().getName();
        testContext
                .given(StackTestDto.class)
                .valid()
                .withName(clusterName)
                .when(stackTestClient.createV4())
                .await(STACK_AVAILABLE)
                .when(stackTestClient.getV4())
                .then(ShowBlueprintUtil::checkGeneratedBlueprint)
                .validate();
    }

    private static void validateGeneratedBlueprint(String extendedBlueprintText) {
        if (Strings.isNullOrEmpty(extendedBlueprintText)) {
            throw new TestFailException("Generated Blueprint does not exist");
        } else if (!JsonUtil.isValid(extendedBlueprintText)) {
            throw new TestFailException("Generated Blueprint is not a valid json");
        }
    }
}
