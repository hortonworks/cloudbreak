package com.sequenceiq.it.cloudbreak.testcase.mock;

import static com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.ENV_STOPPED;
import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status.AVAILABLE;
import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status.STOPPED;
import static com.sequenceiq.it.cloudbreak.context.RunningParameter.emptyRunningParameter;
import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;
import static com.sequenceiq.it.cloudbreak.context.RunningParameter.pollingInterval;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus;
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
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;

public class MockSdxPemTests extends AbstractMockTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(MockSdxPemTests.class);

    private static final Duration POLLING_INTERVAL = Duration.of(3000, ChronoUnit.MILLIS);

    private static final String DX_1 = "dx1";

    @Inject
    private SdxTestClient sdxTestClient;

    @Inject
    private FreeIpaTestClient freeIpaTestClient;

    @Inject
    private EnvironmentTestClient environmentTestClient;

    @Inject
    private DistroXTestClient distroXTestClient;

    @Inject
    private EnvironmentAuditGrpcServiceAssertion auditGrpcServiceAssertion;

    @Inject
    private EnvironmentListStructuredEventAssertions environmentListStructuredEventAssertions;

    @Override
    protected void setupTest(TestContext testContext) {
        // PEM certGenerationEnabled is expected for these tests - gateway.cert.generation.enabled
        createDefaultUser(testContext);
        createDefaultCredential(testContext);
        createDefaultImageCatalog(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running Cloudbreak",
            when = "a valid SDX Internal Create request is sent but DNS registration was not successful",
            then = "SDX should be in PROVISIONING_FAILED / CREATE_FAILED state when finished with statusReason: " +
                    "'Failed to create or update DNS entry for endpoint'"
    )
    public void testProvisionSdxWithDeniedDnsRequest(MockedTestContext testContext) {
        String sdxInternal = resourcePropertyProvider().getName();

        testContext
                .given(EnvironmentTestDto.class)
                    .withNetwork()
                    .withCreateFreeIpa(Boolean.FALSE)
                    // denydns is checked in MockPublicEndpointManagementService.java
                    .withName(resourcePropertyProvider().getEnvironmentName("denydns"))
                    .when(getEnvironmentTestClient().create())
                    .await(EnvironmentStatus.AVAILABLE)
                .given(FreeIpaTestDto.class)
                    .when(freeIpaTestClient.create())
                    .await(AVAILABLE)
                .given(sdxInternal, SdxInternalTestDto.class)
                .when(sdxTestClient.createInternal(), key(sdxInternal))
                    .awaitForFlowFail()
                .when(sdxTestClient.describeInternal(), key(sdxInternal))
                    .then((tc, testDto, client) -> {
                        checkExpectedSdxStatusReason(testDto);
                        checkExpectedSdxClusterStatus(testDto, SdxClusterStatusResponse.PROVISIONING_FAILED);
                        checkExpectedSdxStackv4Status(testDto, Status.CREATE_FAILED);
                        return testDto;
                    })
                .given(EnvironmentTestDto.class)
                .when(getEnvironmentTestClient().describe())
                    .then((tc, testDto, client) -> {
                        checkExpectedEnvironmentStatus(testDto, EnvironmentStatus.AVAILABLE);
                        return testDto;
                    })
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running Cloudbreak",
            when = " stop the Env/DL/DH then during restart the internal DNS update is not successful",
            then = "start should fail with 'Failed to create or update DNS entry for endpoint'"
    )
    public void testStartClusterWithDeniedDnsRequest(MockedTestContext testContext) {
            createEnvDatalakeDatahub(testContext, "deny2nddnsatdl", false);
            restartEnvDatalakeDatahub(testContext, false)
                .given(SdxInternalTestDto.class)
                    .await(SdxClusterStatusResponse.START_FAILED)
                .when(sdxTestClient.describeInternal())
                .then((tc, testDto, client) -> {
                    checkExpectedSdxStatusReason(testDto);
                    checkExpectedSdxStackv4Status(testDto, Status.START_FAILED);
                    return testDto;
                })
                .given(EnvironmentTestDto.class)
                    .awaitForFlow(emptyRunningParameter().withWaitForFlowFail())
                .when(getEnvironmentTestClient().describe())
                .then((tc, testDto, client) -> {
                    checkExpectedEnvironmentStatus(testDto, EnvironmentStatus.START_DATALAKE_FAILED);
                    return testDto;
                })
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running Cloudbreak",
            when = "restart failed on datalake with dns update",
            then = "perform datalake's update public dns command and wait for a running datalake "
    )
    public void testUpdateDnsEntriesAfterFailedDatalakeStart(MockedTestContext testContext) {
        // deny2nddnsatdl is checked in MockPublicEndpointManagementService.java
        createEnvDatalakeDatahub(testContext, "deny2nddnsatdl", true);
        restartEnvDatalakeDatahub(testContext, true)
                .given(SdxInternalTestDto.class)
                .await(SdxClusterStatusResponse.START_FAILED)
                .when(sdxTestClient.describeInternal())
                .then((tc, testDto, client) -> {
                    checkExpectedSdxStatusReason(testDto);
                    checkExpectedSdxStackv4Status(testDto, Status.START_FAILED);
                    return testDto;
                })
                .given(EnvironmentTestDto.class)
                .awaitForFlow(emptyRunningParameter().withWaitForFlowFail())
                .when(getEnvironmentTestClient().describe())
                .then((tc, testDto, client) -> {
                    checkExpectedEnvironmentStatus(testDto, EnvironmentStatus.START_DATALAKE_FAILED);
                    return testDto;
                })
                .given(SdxInternalTestDto.class)
                .when(sdxTestClient.updatePublicDnsEntriesInternal())
                    .await(SdxClusterStatusResponse.RUNNING)
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running Cloudbreak",
            when = "restart (with datahub start separately) and wait for failed start flow on datahub",
            then = "perform datahub's update public dns command and validate we do not get any error"
    )
    public void testUpdateDnsEntriesAfterFailedDatahubStart(MockedTestContext testContext) {
        // deny2nddnsatdh is checked in MockPublicEndpointManagementService.java
        createEnvDatalakeDatahub(testContext, "deny2nddnsatdh", false);
        restartEnvDatalakeWithoutDatahub(testContext)
                .given(SdxInternalTestDto.class)
                .await(SdxClusterStatusResponse.RUNNING)
                .given(EnvironmentTestDto.class)
                .awaitForFlow()
                .when(getEnvironmentTestClient().describe())
                .then((tc, testDto, client) -> {
                    checkExpectedEnvironmentStatus(testDto, EnvironmentStatus.AVAILABLE);
                    return testDto;
                })
                .given(DX_1, DistroXTestDto.class)
                .when(distroXTestClient.start())
                .awaitForFlowFail()
                .when(distroXTestClient.updatePublicDnsEntries())
                .awaitForFlow()
                .validate();
        }

    private TestContext createEnvDatalakeDatahub(TestContext testContext, String environmentNamePrefix, boolean skipDatahubCreation) {
        testContext
                .given(EnvironmentNetworkTestDto.class)
                .given(EnvironmentTestDto.class)
                .withNetwork()
                .withCreateFreeIpa(false)
                .withName(resourcePropertyProvider().getEnvironmentName(environmentNamePrefix))
                .when(environmentTestClient.create())
                .await(EnvironmentStatus.AVAILABLE)
                .given(FreeIpaTestDto.class)
                .when(freeIpaTestClient.create())
                .await(AVAILABLE)
                .given(SdxInternalTestDto.class)
                .when(sdxTestClient.createInternal())
                .await(SdxClusterStatusResponse.RUNNING);
        if (!skipDatahubCreation) {
            testContext
                    .given(DX_1, DistroXTestDto.class)
                    .when(distroXTestClient.create(), key(DX_1))
                    .given(DX_1, DistroXTestDto.class)
                    .await(STACK_AVAILABLE, key(DX_1));
        }
        return testContext;
    }

    private TestContext restartEnvDatalakeDatahub(TestContext testContext, boolean skipDatahuCheck) {
        testContext
                .given(EnvironmentTestDto.class)
                .when(environmentTestClient.stop());

        if (!skipDatahuCheck) {
            testContext
                .given(DX_1, DistroXTestDto.class)
                .await(STACK_STOPPED, key(DX_1));
        }

        testContext
                .given(SdxInternalTestDto.class)
                .await(SdxClusterStatusResponse.STOPPED)
                .given(FreeIpaTestDto.class)
                .await(STOPPED)
                .given(EnvironmentTestDto.class)
                .await(ENV_STOPPED, pollingInterval(POLLING_INTERVAL))
                .when(environmentTestClient.start());
        return testContext;
    }

    private TestContext restartEnvDatalakeWithoutDatahub(TestContext testContext) {
        testContext
                .given(EnvironmentTestDto.class)
                .when(environmentTestClient.stop())
                .given(DX_1, DistroXTestDto.class)
                .await(STACK_STOPPED, key(DX_1))
                .given(SdxInternalTestDto.class)
                .await(SdxClusterStatusResponse.STOPPED)
                .given(FreeIpaTestDto.class)
                .await(STOPPED)
                .given(EnvironmentTestDto.class)
                .await(ENV_STOPPED, pollingInterval(POLLING_INTERVAL))
                .when(environmentTestClient.startWithoutDatahub());
        return testContext;

    }

    private void checkExpectedSdxStatusReason(SdxInternalTestDto testDto) {
        String statusReason = testDto.getResponse().getStackV4Response().getStatusReason();
        String expectedStatusReason = "Failed to create or update DNS entry for endpoint";
        Log.expect(LOGGER, "Expected statusReason contains: '" + expectedStatusReason + "' actual statusReason: '" + statusReason + "'");
        if (!statusReason.contains(expectedStatusReason)) {
            throw new TestFailException("Test failed to reach expected StatusReason: " + expectedStatusReason + " got: " + statusReason);
        }
    }

    private void checkExpectedSdxClusterStatus(SdxInternalTestDto testDto, SdxClusterStatusResponse expectedStatus) {
        SdxClusterStatusResponse status = testDto.getResponse().getStatus();
        Log.expect(LOGGER, "Expected sdx cluster status: '" + expectedStatus + "' actual statusReason: '" + status + "'");
        if (!(status == expectedStatus)) {
            throw new TestFailException("Test failed to reach sdx cluster status: " + expectedStatus + " got: " + status);
        }
    }

    private void checkExpectedSdxStackv4Status(SdxInternalTestDto testDto, Status expectedStatus) {
        Status status = testDto.getResponse().getStackV4Response().getStatus();
        Log.expect(LOGGER, "Expected sdxStackV4 status: '" + expectedStatus + "' actual statusReason: '" + status + "'");
        if (!(status == expectedStatus)) {
            throw new TestFailException("Test failed to reach expected sdxStackV4 status: " + expectedStatus + " got: " + status);
        }
    }

    private void checkExpectedEnvironmentStatus(EnvironmentTestDto testDto, EnvironmentStatus expectedStatus) {
        EnvironmentStatus status = testDto.getResponse().getEnvironmentStatus();
        Log.expect(LOGGER, "Expected environment's status: '" + expectedStatus + "' actual statusReason: '" + status + "'");
        if (!(status == expectedStatus)) {
            throw new TestFailException("Test failed to reach environment's status:" + expectedStatus + " got: " + status);
        }
    }
}
