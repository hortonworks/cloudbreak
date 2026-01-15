package com.sequenceiq.it.cloudbreak.testcase.e2e.gov;

import org.testng.annotations.Test;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationState;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentNetworkTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaUserSyncTestDto;
import com.sequenceiq.it.cloudbreak.dto.telemetry.TelemetryTestDto;

public class BasicFreeIpaTests extends PreconditionGovTest {

    @Test(dataProvider = TEST_CONTEXT)
    @Description(
            given = "there is an available environment with CCM2 and no FreeIpa",
            when = "creating a new FreeIpa with two instances",
            then = "FreeIpa should be created successfuly",
                and = "all users should be syncronised successfully in 5 minutes")
    public void testFreeIpaWithTwoInstances(TestContext testContext) {
        testContext
                .given(EnvironmentNetworkTestDto.class)
                .given("telemetry", TelemetryTestDto.class)
                    .withLogging()
                    .withReportClusterLogs()
                .given(EnvironmentTestDto.class)
                    .withNetwork()
                    .withTelemetry("telemetry")
                    .withTunnel(testContext.getTunnel())
                    .withResourceEncryption(testContext.isResourceEncryptionEnabled())
                    .withFreeIpa(attachedFreeIpaHARequestForTest())
                .when(getEnvironmentTestClient().create())
                .awaitForCreationFlow()
                .given(FreeIpaTestDto.class)
                    .withEnvironment()
                .when(getFreeIpaTestClient().describe())
                .await(Status.AVAILABLE)
                .awaitForHealthyInstances()
                .given(FreeIpaUserSyncTestDto.class)
                .when(getFreeIpaTestClient().getLastSyncOperationStatus())
                .await(OperationState.COMPLETED)
                .when(getFreeIpaTestClient().syncAll())
                .await(OperationState.COMPLETED)
                .validate();
    }
}
