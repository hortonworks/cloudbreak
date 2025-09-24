package com.sequenceiq.it.cloudbreak.testcase.mock;

import jakarta.inject.Inject;

import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.client.DistroXTestClient;
import com.sequenceiq.it.cloudbreak.client.EnvironmentTestClient;
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.context.RunningParameter;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;

public class EnvironmentDatalakeClusterTest extends AbstractMockTest {

    @Inject
    private EnvironmentTestClient environmentTestClient;

    @Inject
    private SdxTestClient sdxTestClient;

    @Inject
    private DistroXTestClient distroXTestClient;

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "Create datalake cluster and then delete",
            when = "create cluster and if available then delete",
            then = "the cluster should work")
    public void testCreateDatalakeDelete(MockedTestContext testContext) {
        testContext
                .given(SdxInternalTestDto.class)
                .withEnvironment()
                .when(sdxTestClient.createInternal())
                .awaitForFlow()
                .given(DistroXTestDto.class)
                .withEnvironment()
                .when(distroXTestClient.create())
                .awaitForFlow()
                .when(distroXTestClient.delete(), RunningParameter.withoutLogError())
                .await(STACK_DELETED)
                .given(EnvironmentTestDto.class)
                .when(environmentTestClient.describe())
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "Create datalake cluster and workload",
            when = "call create cluster with datalake and with workload config",
            then = "will work fine")
    public void testSameEnvironmentInDatalakeAndWorkload(MockedTestContext testContext) {
        testContext
                .given(SdxInternalTestDto.class)
                .withEnvironment()
                .when(sdxTestClient.createInternal())
                .awaitForFlow()
                .given(DistroXTestDto.class)
                .withEnvironment()
                .when(distroXTestClient.create())
                .validate();
    }
}
