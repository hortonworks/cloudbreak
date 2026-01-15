package com.sequenceiq.it.cloudbreak.testcase.e2e.gov;

import org.testng.annotations.Test;

import com.sequenceiq.freeipa.api.v1.operation.model.OperationState;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaUserSyncTestDto;
import com.sequenceiq.it.cloudbreak.dto.telemetry.TelemetryTestDto;
import com.sequenceiq.it.cloudbreak.util.spot.UseSpotInstances;

public class BasicEnvironmentTests extends PreconditionGovTest {

    @Test(dataProvider = TEST_CONTEXT)
    @UseSpotInstances
    @Description(
            given = "there is a running cloudbreak",
            when = "creating a new Environment with CCM2 and no FreeIpa",
            then = "Environment should be created successfuly and get in AVAILABLE state")
    public void testCreateEnvironmentWithNoFreeIpa(TestContext testContext) {
        testContext
                .given("telemetry", TelemetryTestDto.class)
                    .withLogging()
                    .withReportClusterLogs()
                .given(EnvironmentTestDto.class)
                    .withNetwork()
                    .withTelemetry("telemetry")
                    .withResourceEncryption(testContext.isResourceEncryptionEnabled())
                    .withTunnel(testContext.getTunnel())
                    .withCreateFreeIpa(Boolean.FALSE)
                .when(getEnvironmentTestClient().create())
                .given(EnvironmentTestDto.class)
                .awaitForCreationFlow()
                .when(getEnvironmentTestClient().describe())
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT)
    @UseSpotInstances
    @Description(
            given = "there is a running cloudbreak",
            when = "creating a new Environment with CCM2 and FreeIpa",
            then = "Environment should be created successfuly and get in AVAILABLE state")
    public void testCreateEnvironmentWithFreeIpa(TestContext testContext) {
        testContext
                .given("telemetry", TelemetryTestDto.class)
                    .withLogging()
                    .withReportClusterLogs()
                .given(EnvironmentTestDto.class)
                    .withNetwork()
                    .withTelemetry("telemetry")
                    .withResourceEncryption(testContext.isResourceEncryptionEnabled())
                    .withTunnel(testContext.getTunnel())
                    .withCreateFreeIpa(Boolean.TRUE)
                    .withOneFreeIpaNode()
                    .withFreeIpaImage(commonCloudProperties().getImageValidation().getFreeIpaImageCatalog(),
                            commonCloudProperties().getImageValidation().getFreeIpaImageUuid())
                .when(getEnvironmentTestClient().create())
                .given(EnvironmentTestDto.class)
                .awaitForCreationFlow()
                .when(getEnvironmentTestClient().describe())
                .given(FreeIpaUserSyncTestDto.class)
                .when(getFreeIpaTestClient().getLastSyncOperationStatus())
                .await(OperationState.COMPLETED)
                .validate();
    }
}
