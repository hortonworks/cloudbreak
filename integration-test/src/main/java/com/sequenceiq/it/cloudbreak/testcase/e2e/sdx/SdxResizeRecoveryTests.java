package com.sequenceiq.it.cloudbreak.testcase.e2e.sdx;

import static com.sequenceiq.it.cloudbreak.cloud.HostGroupType.MASTER;
import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;

import java.util.List;

import javax.inject.Inject;

import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.dto.telemetry.TelemetryTestDto;
import com.sequenceiq.it.cloudbreak.microservice.SdxClient;
import com.sequenceiq.it.cloudbreak.util.SdxUtil;
import com.sequenceiq.it.cloudbreak.util.resize.SdxResizeTestValidator;
import com.sequenceiq.it.cloudbreak.util.spot.UseSpotInstances;
import com.sequenceiq.sdx.api.model.SdxClusterShape;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;
import com.sequenceiq.sdx.api.model.SdxDatabaseAvailabilityType;
import com.sequenceiq.sdx.api.model.SdxDatabaseRequest;

public class SdxResizeRecoveryTests extends PreconditionSdxE2ETest {
    @Inject
    private SdxTestClient sdxTestClient;

    @Inject
    private SdxUtil sdxUtil;

    @Test(dataProvider = TEST_CONTEXT)
    @UseSpotInstances
    @Description(
            given = "there is a running Cloudbreak, and an SDX cluster in available state",
            when = "resize is performed on the SDX cluster but fails during provisioning",
            then = "recovery should be available and successful when run, the original cluster should be up and running"
    )
    public void testSdxResizeRecoveryFromProvisioningFailure(TestContext testContext) {
        String sdx = resourcePropertyProvider().getName();
        SdxResizeTestValidator resizeTestValidator = new SdxResizeTestValidator(SdxClusterShape.CUSTOM);
        performInitialResizeRecoverySteps(testContext, sdx, resizeTestValidator)
                .then((tc, testDto, client) -> causeProvisioningFailureAndAwait(testDto, sdx))
                .then((tc, testDto, client) -> performResizeRecoveryAndValidate(testDto, sdx, resizeTestValidator))
                .validate();
    }

    private SdxInternalTestDto performInitialResizeRecoverySteps(TestContext testContext, String sdxKey, SdxResizeTestValidator validator) {
        SdxDatabaseRequest sdxDatabaseRequest = new SdxDatabaseRequest();
        sdxDatabaseRequest.setAvailabilityType(SdxDatabaseAvailabilityType.NONE);
        return testContext
                .given("telemetry", TelemetryTestDto.class)
                .withLogging()
                .withReportClusterLogs()
                .given(sdxKey, SdxInternalTestDto.class)
                .withDatabase(sdxDatabaseRequest)
                .withCloudStorage(getCloudStorageRequest(testContext))
                .withClusterShape(SdxClusterShape.CUSTOM)
                .withTelemetry("telemetry")
                .when(sdxTestClient.createInternal(), key(sdxKey))
                .await(SdxClusterStatusResponse.RUNNING, key(sdxKey))
                .awaitForHealthyInstances()
                .then((tc, testDto, client) -> {
                    validator.setExpectedCrn(sdxUtil.getCrn(testDto, client));
                    validator.setExpectedName(testDto.getName());
                    validator.setExpectedRuntime(sdxUtil.getRuntime(testDto, client));
                    validator.setExpectedCreationTimestamp(sdxUtil.getCreated(testDto, client));
                    return testDto;
                })
                .when(sdxTestClient.resize(), key(sdxKey))
                .await(SdxClusterStatusResponse.STOP_IN_PROGRESS, key(sdxKey).withoutWaitForFlow())
                .await(SdxClusterStatusResponse.STACK_CREATION_IN_PROGRESS, key(sdxKey).withoutWaitForFlow());
    }

    private SdxInternalTestDto causeProvisioningFailureAndAwait(SdxInternalTestDto testDto, String sdxKey) {
        return testDto.awaitForInstancesToExist()
                .awaitForStartingInstances()
                .then((tc, dto, client) -> deleteMasterNode(dto, client, tc))
                .await(SdxClusterStatusResponse.PROVISIONING_FAILED, key(sdxKey).withoutWaitForFlow());
    }

    private SdxInternalTestDto performResizeRecoveryAndValidate(SdxInternalTestDto testDto, String sdxKey, SdxResizeTestValidator validator) {
        return testDto.when(sdxTestClient.recoverFromResizeInternal(), key(sdxKey))
                .await(SdxClusterStatusResponse.RUNNING, key(sdxKey))
                .awaitForHealthyInstances()
                .then((tc, dto, client) -> validator.validateRecoveredCluster(dto));
    }

    private SdxInternalTestDto deleteMasterNode(SdxInternalTestDto testDto, SdxClient client, TestContext testContext) {
        List<String> instanceIdsToDelete = sdxUtil.getInstanceIds(testDto, client, MASTER.getName());
        getCloudFunctionality(testContext).deleteInstances(testDto.getName(), instanceIdsToDelete);
        return testDto;
    }
}
