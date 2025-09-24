package com.sequenceiq.it.cloudbreak.testcase.mock;

import jakarta.inject.Inject;

import org.testng.annotations.Test;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.it.cloudbreak.client.DistroXTestClient;
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.util.ShowBlueprintUtil;

public class ShowBlueprintTest extends AbstractMockTest {

    @Inject
    private SdxTestClient sdxTestClient;

    @Inject
    private DistroXTestClient distroXTestClient;

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(given = "stack", when = "cluster does not exist ", then = "we should return with the future blueprint")
    public void testGetBlueprintWhenClusterIsNotAliveThenShouldReturnWithBlueprint(MockedTestContext testContext) {
        String clusterName = resourcePropertyProvider().getName();
        testContext
                .given(DistroXTestDto.class)
                .valid()
                .withName(clusterName)
                .when(distroXTestClient.blueprintRequest())
                .then(ShowBlueprintUtil::checkFutureBlueprint)
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(given = "stack", when = "cluster exist ", then = "we should return with the generated blueprint")
    public void testGetBlueprintWhenClusterIsAliveThenShouldReturnWithBlueprint(MockedTestContext testContext) {
        String clusterName = resourcePropertyProvider().getName();
        testContext
                .given(SdxInternalTestDto.class)
                .when(sdxTestClient.createInternal())
                .awaitForFlow()
                .given(DistroXTestDto.class)
                .valid()
                .withName(clusterName)
                .when(distroXTestClient.create())
                .awaitForFlow()
                .when(distroXTestClient.get())
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
