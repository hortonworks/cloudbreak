package com.sequenceiq.it.cloudbreak.testcase.e2e.freeipa;

import static com.sequenceiq.freeipa.api.v1.operation.model.OperationState.COMPLETED;
import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;
import static com.sequenceiq.it.cloudbreak.context.RunningParameter.waitForFlow;

import javax.inject.Inject;

import org.testng.annotations.Ignore;
import org.testng.annotations.Test;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.it.cloudbreak.client.FreeIpaTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaOperationStatusTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.dto.telemetry.TelemetryTestDto;
import com.sequenceiq.it.cloudbreak.testcase.e2e.AbstractE2ETest;

public class FreeIpaUpgradeTests extends AbstractE2ETest {

    protected static final Status FREEIPA_AVAILABLE = Status.AVAILABLE;

    protected static final Status FREEIPA_DELETE_COMPLETED = Status.DELETE_COMPLETED;

    @Inject
    private FreeIpaTestClient freeIpaTestClient;

    @Test(dataProvider = TEST_CONTEXT)
    @Description(
            given = "there is a running cloudbreak",
            when = "a valid stack create request is sent with 1 FreeIPA instances " +
                    "AND the stack is upgraded one node at a time",
            then = "the stack should be available AND deletable")
    public void testSingleFreeIpaInstanceUpgrade(TestContext testContext) {
        String freeIpa = resourcePropertyProvider().getName();

        testContext
                .given("telemetry", TelemetryTestDto.class)
                .withLogging()
                .withReportClusterLogs()
                .given(freeIpa, FreeIpaTestDto.class)
                .withTelemetry("telemetry")
                .withUpgradeCatalogAndImage()
                .when(freeIpaTestClient.create(), key(freeIpa))
                .await(FREEIPA_AVAILABLE)
                .when(freeIpaTestClient.upgrade())
                .await(Status.UPDATE_IN_PROGRESS, waitForFlow().withWaitForFlow(Boolean.FALSE))
                .given(FreeIpaOperationStatusTestDto.class)
                .withOperationId(((FreeIpaTestDto) testContext.get(freeIpa)).getOperationId())
                .await(COMPLETED)
                .given(freeIpa, FreeIpaTestDto.class)
                .await(FREEIPA_AVAILABLE, waitForFlow().withWaitForFlow(Boolean.FALSE))
                .then((tc, testDto, client) -> freeIpaTestClient.delete().action(tc, testDto, client))
                .await(FREEIPA_DELETE_COMPLETED, waitForFlow().withWaitForFlow(Boolean.FALSE))
                .validate();
    }

    @Ignore("Fails with {status=TIMEDOUT}")
    @Test(dataProvider = TEST_CONTEXT)
    @Description(
            given = "there is a running cloudbreak",
            when = "a valid stack create request is sent with 3 FreeIPA instances " +
                    "AND the stack is upgraded one node at a time",
            then = "the stack should be available AND deletable")
    public void testHAFreeIpaInstanceUpgrade(TestContext testContext) {
        String freeIpa = resourcePropertyProvider().getName();

        testContext
                .given("telemetry", TelemetryTestDto.class)
                .withLogging()
                .withReportClusterLogs()
                .given(freeIpa, FreeIpaTestDto.class)
                .withFreeIpaHa(1, 3)
                .withTelemetry("telemetry")
                .withUpgradeCatalogAndImage()
                .when(freeIpaTestClient.create(), key(freeIpa))
                .await(FREEIPA_AVAILABLE)
                .when(freeIpaTestClient.upgrade())
                .await(Status.UPDATE_IN_PROGRESS, waitForFlow().withWaitForFlow(Boolean.FALSE))
                .given(FreeIpaOperationStatusTestDto.class)
                .withOperationId(((FreeIpaTestDto) testContext.get(freeIpa)).getOperationId())
                .await(COMPLETED)
                .given(freeIpa, FreeIpaTestDto.class)
                .await(FREEIPA_AVAILABLE, waitForFlow().withWaitForFlow(Boolean.FALSE))
                .then((tc, testDto, client) -> freeIpaTestClient.delete().action(tc, testDto, client))
                .await(FREEIPA_DELETE_COMPLETED, waitForFlow().withWaitForFlow(Boolean.FALSE))
                .validate();
    }
}
