package com.sequenceiq.it.cloudbreak.testcase.e2e.distrox;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.pollingInterval;

import java.time.Duration;
import java.util.Map;
import java.util.stream.IntStream;

import jakarta.inject.Inject;

import org.testng.ITestContext;
import org.testng.annotations.Test;

import com.sequenceiq.distrox.api.v1.distrox.model.database.DistroXDatabaseAvailabilityType;
import com.sequenceiq.distrox.api.v1.distrox.model.database.DistroXDatabaseRequest;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationState;
import com.sequenceiq.it.cloudbreak.client.DistroXTestClient;
import com.sequenceiq.it.cloudbreak.client.EnvironmentTestClient;
import com.sequenceiq.it.cloudbreak.client.FreeIpaTestClient;
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentNetworkTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaUserSyncTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxTestDto;
import com.sequenceiq.it.cloudbreak.dto.telemetry.TelemetryTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.testcase.e2e.AbstractE2ETest;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;

public class DistroXBigScaleTest extends AbstractE2ETest {

    private static final Duration BIG_SCALE_POLLING_INTERVAL = Duration.ofSeconds(30L);

    private static final Map<String, String> COST_REDUCER_TAGS = Map.of("cloud-cost-reducer-ignore", "true");

    @Inject
    private DistroXTestClient distroXTestClient;

    @Inject
    private EnvironmentTestClient environmentTestClient;

    @Inject
    private FreeIpaTestClient freeIpaTestClient;

    @Inject
    private SdxTestClient sdxTestClient;

    @Override
    protected void setupTest(TestContext testContext) {
        testContext.getCloudProvider().getCloudFunctionality().cloudStorageInitialize();
        createDefaultUser(testContext);
        initializeDefaultBlueprints(testContext);
        createDefaultCredential(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT)
    @Description(
            given = "there is a valid credential for the selected provider",
            when = "new environment with attached freeIpa and SDX should be created along with Cloud Cost Reducer tags",
                and = "new distroX also should be created for environment",
            then = "distrox can be scaled up (up to 800 Worker nodes) then down (down to 600 Worker nodes) in 3 rounds")
    public void testCreateAndBigScaleDistroX(TestContext testContext, ITestContext iTestContext) {
        DistroXScaleTestParameters params = new DistroXScaleTestParameters(iTestContext.getCurrentXmlTest().getAllParameters());
        DistroXDatabaseRequest distroXDatabaseRequest = new DistroXDatabaseRequest();
        distroXDatabaseRequest.setAvailabilityType(DistroXDatabaseAvailabilityType.HA);

        if (params.getTimes() < 1) {
            throw new TestFailException("Test should execute at least 1 round of scaling");
        }

        testContext
                .given(EnvironmentNetworkTestDto.class)
                .given("telemetry", TelemetryTestDto.class)
                    .withLogging()
                    .withReportClusterLogs()
                .given(EnvironmentTestDto.class)
                    .withNetwork()
                    .withTelemetry("telemetry")
                    .withTunnel(testContext.getTunnel())
                    .addTags(COST_REDUCER_TAGS)
                    .withCreateFreeIpa(Boolean.TRUE)
                    .withOneFreeIpaNode()
                .when(environmentTestClient.create())
                .awaitForCreationFlow()
                .given(FreeIpaUserSyncTestDto.class)
                .when(freeIpaTestClient.getLastSyncOperationStatus())
                .await(OperationState.COMPLETED)
                .given(EnvironmentTestDto.class)
                .when(environmentTestClient.describe())
                .given(SdxTestDto.class)
                    .withCloudStorage()
                    .withEnvironment()
                    .withTags(COST_REDUCER_TAGS)
                .when(sdxTestClient.create())
                .await(SdxClusterStatusResponse.RUNNING)
                .awaitForHealthyInstances()
                .given(DistroXTestDto.class)
                    .withEnvironment()
                    .withExternalDatabase(distroXDatabaseRequest)
                    .addTags(COST_REDUCER_TAGS)
                .when(distroXTestClient.create())
                .await(STACK_AVAILABLE)
                .awaitForHealthyInstances()
                .validate();
        IntStream.range(1, params.getTimes()).forEach(i ->
                testContext
                        .given(DistroXTestDto.class)
                        .when(distroXTestClient.scale(params.getHostGroup(), params.getScaleUpTarget() * i))
                        .awaitForFlow(pollingInterval(BIG_SCALE_POLLING_INTERVAL))
                        .validate());
        IntStream.range(1, params.getTimes()).forEach(i ->
                testContext
                        .given(DistroXTestDto.class)
                        .when(distroXTestClient.scale(params.getHostGroup(), params.getScaleUpTarget()))
                        .awaitForFlow(pollingInterval(BIG_SCALE_POLLING_INTERVAL))
                        .when(distroXTestClient.scale(params.getHostGroup(), params.getScaleDownTarget()))
                        .awaitForFlow(pollingInterval(BIG_SCALE_POLLING_INTERVAL))
                        .validate());
    }
}
