package com.sequenceiq.it.cloudbreak.testcase.e2e.freeipa;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;
import static com.sequenceiq.it.cloudbreak.context.RunningParameter.waitForFlow;

import java.util.Set;

import javax.inject.Inject;

import org.testng.annotations.Test;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceMetadataType;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceStatus;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationState;
import com.sequenceiq.it.cloudbreak.client.FreeIpaTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaUserSyncTestDto;
import com.sequenceiq.it.cloudbreak.dto.telemetry.TelemetryTestDto;
import com.sequenceiq.it.cloudbreak.testcase.e2e.AbstractE2ETest;

public class FreeIpaTests extends AbstractE2ETest {

    protected static final Status FREEIPA_AVAILABLE = Status.AVAILABLE;

    protected static final Status FREEIPA_DELETE_COMPLETED = Status.DELETE_COMPLETED;

    @Inject
    private FreeIpaTestClient freeIpaTestClient;

    @Test(dataProvider = TEST_CONTEXT)
    @Description(
            given = "there is a running cloudbreak",
            when = "a valid stack create request is sent with 2 FreeIPA instances " +
                    "AND the stack is stopped " +
                    "AND the stack is started " +
                    "AND the stack is repaired one node at a time",
            then = "the stack should be available AND deletable")
    public void testCreateStopStartRepairFreeIpaWithTwoInstances(TestContext testContext) {
        String freeIpa = resourcePropertyProvider().getName();

        int instanceGroupCount = 1;
        int instanceCountByGroup = 2;

        testContext
                .given("telemetry", TelemetryTestDto.class)
                .withLogging()
                .withReportClusterLogs()
                .given(freeIpa, FreeIpaTestDto.class)
                .withFreeIpaHa(instanceGroupCount, instanceCountByGroup)
                .withTelemetry("telemetry")
                .when(freeIpaTestClient.create(), key(freeIpa))
                .await(FREEIPA_AVAILABLE)
                .when(freeIpaTestClient.stop())
                .await(Status.STOPPED)
                .when(freeIpaTestClient.start())
                .await(Status.AVAILABLE)
                .awaitForHealthyInstances(key(freeIpa).withIgnoredStatues(Set.of(InstanceStatus.UNHEALTHY)))
                .when(freeIpaTestClient.repair(InstanceMetadataType.GATEWAY_PRIMARY))
                .await(Status.UPDATE_IN_PROGRESS, waitForFlow().withWaitForFlow(Boolean.FALSE))
                .await(FREEIPA_AVAILABLE)
                .awaitForHealthyInstances(key(freeIpa).withIgnoredStatues(Set.of(InstanceStatus.UNHEALTHY)))
                .when(freeIpaTestClient.repair(InstanceMetadataType.GATEWAY))
                .await(Status.UPDATE_IN_PROGRESS, waitForFlow().withWaitForFlow(Boolean.FALSE))
                .await(FREEIPA_AVAILABLE)
                .awaitForHealthyInstances()
                .given(FreeIpaUserSyncTestDto.class)
                .forAllEnvironments()
                .when(freeIpaTestClient.getLastSyncOperationStatus())
                .await(OperationState.COMPLETED)
                .when(freeIpaTestClient.syncAll())
                .await(OperationState.COMPLETED)
                .given(freeIpa, FreeIpaTestDto.class)
                .then((tc, testDto, client) -> freeIpaTestClient.delete().action(tc, testDto, client))
                .await(FREEIPA_DELETE_COMPLETED)
                .validate();
    }
}
