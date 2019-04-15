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
import com.sequenceiq.it.cloudbreak.newway.util.ShowClusterDefinitionUtil;

public class ShowClusterDefinitionTest extends AbstractIntegrationTest {

    @Inject
    private StackTestClient stackTestClient;

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(given = "stack", when = "cluster does not exist ", then = "we should return with the future cluster definition")
    public void testGetClusterDefinitionWhenClusterIsNotAliveThenShouldReturnWithClusterDefinition(MockedTestContext testContext) {
        String clusterName = resourcePropertyProvider().getName();
        testContext
                .given(StackTestDto.class)
                .valid()
                .withName(clusterName)
                .when(stackTestClient.clusterDefinitionRequestV4())
                .then(ShowClusterDefinitionUtil::checkFutureClusterDefinition)
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(given = "stack", when = "cluster exist ", then = "we should return with the generated cluster definition")
    public void testGetClusterDefinitionWhenClusterIsAliveThenShouldReturnWithClusterDefinition(MockedTestContext testContext) {
        String clusterName = resourcePropertyProvider().getName();
        testContext
                .given(StackTestDto.class)
                .valid()
                .withName(clusterName)
                .when(stackTestClient.createV4())
                .await(STACK_AVAILABLE)
                .when(stackTestClient.getV4())
                .then(ShowClusterDefinitionUtil::checkGeneratedClusterDefinition)
                .validate();
    }

    private static void validateGeneratedClusterDefinition(String extendedClusterDefinitionText) {
        if (Strings.isNullOrEmpty(extendedClusterDefinitionText)) {
            throw new TestFailException("Generated Cluster Definition does not exist");
        } else if (!JsonUtil.isValid(extendedClusterDefinitionText)) {
            throw new TestFailException("Generated Cluster Definition is not a valid json");
        }
    }
}
