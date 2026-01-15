package com.sequenceiq.it.cloudbreak.testcase.mock;

import static com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.ENV_STOPPED;
import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status.AVAILABLE;
import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;
import static com.sequenceiq.it.cloudbreak.context.RunningParameter.pollingInterval;
import static com.sequenceiq.it.cloudbreak.context.RunningParameter.waitForFlowFail;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

import jakarta.inject.Inject;

import org.testng.annotations.Test;

import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.it.cloudbreak.assertion.audit.EnvironmentAuditGrpcServiceAssertion;
import com.sequenceiq.it.cloudbreak.assertion.environment.EnvironmentListStructuredEventAssertions;
import com.sequenceiq.it.cloudbreak.client.DistroXTestClient;
import com.sequenceiq.it.cloudbreak.client.EnvironmentTestClient;
import com.sequenceiq.it.cloudbreak.client.FreeIpaTestClient;
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentNetworkTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;

public class EnvironmentStartStopTest extends AbstractMockTest {

    private static final Duration POLLING_INTERVAL = Duration.of(3000, ChronoUnit.MILLIS);

    private static final String DX_1 = "dx1";

    private static final String DX_2 = "dx2";

    @Inject
    private EnvironmentTestClient environmentTestClient;

    @Inject
    private FreeIpaTestClient freeIpaTestClient;

    @Inject
    private SdxTestClient sdxTestClient;

    @Inject
    private DistroXTestClient distroXTestClient;

    @Inject
    private EnvironmentListStructuredEventAssertions environmentListStructuredEventAssertions;

    @Inject
    private EnvironmentAuditGrpcServiceAssertion auditGrpcServiceAssertion;

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
            when = "create an env with freeipa, sdx and dh",
            then = "these should be available")
    public void testCreateEnvironment(MockedTestContext testContext) {
        testContext
                .given(EnvironmentNetworkTestDto.class)
                .given(EnvironmentTestDto.class).withNetwork().withCreateFreeIpa(false)
                .when(environmentTestClient.create())
                .awaitForCreationFlow()
                .given(FreeIpaTestDto.class)
                .when(freeIpaTestClient.create())
                .awaitForCreationFlow()
                .given(SdxInternalTestDto.class)
                .when(sdxTestClient.createInternal())
                .awaitForFlow(key(resourcePropertyProvider().getName()))
                .await(SdxClusterStatusResponse.RUNNING)
                .given(DistroXTestDto.class)
                .when(distroXTestClient.create())
                .await(STACK_AVAILABLE)
                .awaitForHealthyInstances()
                .given(EnvironmentTestDto.class)
                .when(environmentTestClient.delete())
                .await(EnvironmentStatus.ARCHIVED, pollingInterval(POLLING_INTERVAL))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running cloudbreak",
            when = "create an attached SDX and Datahub",
            then = "should be stopped first and started after it and validate the flow events")
    public void testCreateStopStartEnvironment(MockedTestContext testContext) {
        testContext
                .given(EnvironmentNetworkTestDto.class)
                .given(EnvironmentTestDto.class).withNetwork().withCreateFreeIpa(false)
                .when(environmentTestClient.create())
                .awaitForCreationFlow()
                .given(FreeIpaTestDto.class)
                .when(freeIpaTestClient.create())
                .awaitForCreationFlow()
                .given(SdxInternalTestDto.class)
                .when(sdxTestClient.createInternal())
                .await(SdxClusterStatusResponse.RUNNING)
                .given(DX_1, DistroXTestDto.class)
                .when(distroXTestClient.create(), key(DX_1))
                .given(DX_2, DistroXTestDto.class)
                .when(distroXTestClient.create(), key(DX_2))
                .given(DX_1, DistroXTestDto.class)
                .await(STACK_AVAILABLE, key(DX_1))
                .given(DX_2, DistroXTestDto.class)
                .await(STACK_AVAILABLE, key(DX_2))
                .given(EnvironmentTestDto.class)
                .when(environmentTestClient.stop())
                // await stopped datahubs
                .given(DX_1, DistroXTestDto.class)
                .await(STACK_STOPPED, key(DX_1))
                .given(DX_2, DistroXTestDto.class)
                .await(STACK_STOPPED, key(DX_2))
                // await stopped datalake
                .given(SdxInternalTestDto.class)
                .await(SdxClusterStatusResponse.STOPPED)
                // await stopped freeipa
                .given(FreeIpaTestDto.class)
                .await(Status.STOPPED)
                // await stopped env
                .given(EnvironmentTestDto.class)
                .await(ENV_STOPPED, pollingInterval(POLLING_INTERVAL))
                .when(environmentTestClient.start())
                // await started freeipa
                .given(FreeIpaTestDto.class)
                .await(AVAILABLE)
                // await started datalake
                .given(SdxInternalTestDto.class)
                .await(SdxClusterStatusResponse.RUNNING)
                // await started datahubs
                .given(DX_1, DistroXTestDto.class)
                .await(STACK_AVAILABLE, key(DX_1))
                .given(DX_2, DistroXTestDto.class)
                .await(STACK_AVAILABLE, key(DX_2))
                // await started env
                .given(EnvironmentTestDto.class)
                .await(EnvironmentStatus.AVAILABLE, pollingInterval(POLLING_INTERVAL))
                .when(environmentTestClient.delete())
                .await(EnvironmentStatus.ARCHIVED, pollingInterval(POLLING_INTERVAL))
                .then(auditGrpcServiceAssertion::create)
                .then(auditGrpcServiceAssertion::delete)
                .then(auditGrpcServiceAssertion::start)
                .then(auditGrpcServiceAssertion::stop)
                .then(environmentListStructuredEventAssertions::checkCreateEvents)
                .then(environmentListStructuredEventAssertions::checkStopEvents)
                .then(environmentListStructuredEventAssertions::checkStartEvents)
                .then(environmentListStructuredEventAssertions::checkDeleteEvents)
                .validate();
    }

    //@Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running cloudbreak",
            when = "create an env with freeipa",
            then = "stop the env, but failed and start it successfully. After this, do the same but with start")
    public void testStopStartEnvironmentWithStopFailed(MockedTestContext testContext) {
        testContext
                .given(EnvironmentNetworkTestDto.class)
                .given(EnvironmentTestDto.class).withNetwork().withCreateFreeIpa(false)
                .when(environmentTestClient.create())
                .awaitForCreationFlow()
                .given(FreeIpaTestDto.class)
                .when(freeIpaTestClient.create())
                .awaitForCreationFlow()
                .mockSpi().stopInstances().post().thenReturn("error", 400, 1)
                .given(EnvironmentTestDto.class)
                .when(environmentTestClient.stop())
                .awaitForFlow(waitForFlowFail())
                .when(environmentTestClient.start())
                .await(EnvironmentStatus.AVAILABLE)
                //do the same with start
                .given(FreeIpaTestDto.class)
                .mockSpi().startInstances().post().thenReturn("error", 400, 1)
                .given(EnvironmentTestDto.class)
                .when(environmentTestClient.stop())
                .await(ENV_STOPPED)
                .when(environmentTestClient.start())
                .awaitForFlow(waitForFlowFail())
                .when(environmentTestClient.stop())
                .await(ENV_STOPPED)
                .validate();
    }
}
