package com.sequenceiq.it.cloudbreak.testcase.mock.clouderamanager;

import jakarta.inject.Inject;

import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.client.DistroXTestClient;
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.testcase.mock.AbstractMockTest;

public class ClouderaManagerSetupTest extends AbstractMockTest {

    @Inject
    private DistroXTestClient distroXTestClient;

    @Inject
    private SdxTestClient sdxTestClient;

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "a working environment",
            when = "a stack is created",
            then = "ClouderaManager user endpoints should be invoked with the proper requests")
    public void verifyCallsAgainstCmUserCreation(MockedTestContext testContext) {
        testContext
                .given(SdxInternalTestDto.class)
                .when(sdxTestClient.createInternal())
                .awaitForFlow()
                .given(DistroXTestDto.class)
                .when(distroXTestClient.create())
                .awaitForFlow()
                .enableVerification()
                .await(STACK_AVAILABLE)
                .mockCm().users().get().atLeast(1).verify()
                .mockCm().users().put().pathVariable("users", "admin").times(1).verify()
                .validate();
    }
}
