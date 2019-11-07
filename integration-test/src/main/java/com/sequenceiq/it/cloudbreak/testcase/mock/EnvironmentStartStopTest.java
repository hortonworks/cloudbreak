package com.sequenceiq.it.cloudbreak.testcase.mock;

import javax.inject.Inject;

import org.testng.annotations.Test;

import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus;
import com.sequenceiq.it.cloudbreak.client.DistroXTestClient;
import com.sequenceiq.it.cloudbreak.client.EnvironmentTestClient;
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.RunningParameter;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentNetworkTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.testcase.AbstractIntegrationTest;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;

public class EnvironmentStartStopTest extends AbstractIntegrationTest {

    @Inject
    private EnvironmentTestClient environmentTestClient;

    @Inject
    private SdxTestClient sdxTestClient;

    @Inject
    private DistroXTestClient distroXTestClient;

    @Override
    protected void setupTest(TestContext testContext) {
        createDefaultUser(testContext);
        createDefaultCredential(testContext);
        createDefaultImageCatalog(testContext);
        initializeDefaultBlueprints(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running cloudbreak",
            when = "create an attached SDX and Datahub",
            then = "should be stopped first and started after it")
    public void testCreateStopStartEnvironment(TestContext testContext) {
        testContext
                .given(EnvironmentNetworkTestDto.class)
                .given(EnvironmentTestDto.class).withNetwork().withCreateFreeIpa(true)
                .when(environmentTestClient.create())
                .await(EnvironmentStatus.AVAILABLE)
                .given(SdxInternalTestDto.class)
                .when(sdxTestClient.createInternal())
                .await(SdxClusterStatusResponse.RUNNING)
                .given("dx1", DistroXTestDto.class)
                .when(distroXTestClient.create(), RunningParameter.key("dx1"))
                .given("dx2", DistroXTestDto.class)
                .when(distroXTestClient.create(), RunningParameter.key("dx2"))
                .await(STACK_AVAILABLE, RunningParameter.key("dx1"))
                .await(STACK_AVAILABLE, RunningParameter.key("dx2"))
                .given(EnvironmentTestDto.class)
                .when(environmentTestClient.stop())
                .await(EnvironmentStatus.ENV_STOPPED)
                .when(environmentTestClient.start())
                .await(EnvironmentStatus.AVAILABLE)
                .validate();
    }
}