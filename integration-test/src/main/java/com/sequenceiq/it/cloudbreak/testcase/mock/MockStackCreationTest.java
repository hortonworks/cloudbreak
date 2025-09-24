package com.sequenceiq.it.cloudbreak.testcase.mock;

import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;

import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.client.DistroXTestClient;
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;

public class MockStackCreationTest extends AbstractMockTest {

    @Inject
    private SdxTestClient sdxTestClient;

    @Inject
    private DistroXTestClient distroXTestClient;

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "a valid stack request",
            when = "create stack twice",
            then = "getting BadRequestException in the second time because the names are same")
    public void testAttemptToCreateTwoRegularClusterWithTheSameName(MockedTestContext testContext) {
        testContext
                .given(SdxInternalTestDto.class)
                .withEnvironment()
                .when(sdxTestClient.createInternal())
                .awaitForFlow()
                .given(DistroXTestDto.class)
                .when(distroXTestClient.create())
                .whenException(distroXTestClient.create(), BadRequestException.class)
                .validate();
    }
}
