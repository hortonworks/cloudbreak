package com.sequenceiq.it.cloudbreak.util.resize;

import static com.sequenceiq.it.cloudbreak.cloud.HostGroupType.MASTER;
import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;

import java.util.List;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.dto.telemetry.TelemetryTestDto;
import com.sequenceiq.it.cloudbreak.util.SdxUtil;
import com.sequenceiq.sdx.api.model.SdxCloudStorageRequest;
import com.sequenceiq.sdx.api.model.SdxClusterShape;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;
import com.sequenceiq.sdx.api.model.SdxDatabaseAvailabilityType;
import com.sequenceiq.sdx.api.model.SdxDatabaseRequest;

@Component
public class SdxResizeTestUtil {

    private static final String MDL_RESIZE_FIXED_RUNTIME_VERSION = "7.2.17";

    @Inject
    private SdxTestClient sdxTestClient;

    @Inject
    private SdxUtil sdxUtil;

    public void runResizeTest(TestContext testContext, String sdxKey, SdxCloudStorageRequest cloudStorageRequest) {
        SdxResizeTestValidator resizeTestValidator = new SdxResizeTestValidator(SdxClusterShape.ENTERPRISE);

        performInitialResizeSteps(testContext, sdxKey, cloudStorageRequest, resizeTestValidator, false)
                .await(SdxClusterStatusResponse.RUNNING, key(sdxKey))
                .awaitForHealthyInstances()

                // Ensure new cluster is of the right shape and has carried over the necessary info from the original cluster.
                .then((tc, dto, client) -> resizeTestValidator.validateResizedCluster(dto))
                .validate();
    }

    public void runResizeRecoveryFromProvisioningFailureTest(TestContext testContext, String sdxKey, SdxCloudStorageRequest cloudStorageRequest) {
        SdxResizeTestValidator resizeTestValidator = new SdxResizeTestValidator(SdxClusterShape.CUSTOM);

        performInitialResizeSteps(testContext, sdxKey, cloudStorageRequest, resizeTestValidator, true)
                .then((tc, testDto, client) -> performProvisioningFailureAndRecovery(testDto, sdxKey))

                // Ensure current cluster is identical to original cluster.
                .then((tc, dto, client) -> resizeTestValidator.validateRecoveredCluster(dto))
                .validate();
    }

    private SdxInternalTestDto performInitialResizeSteps(TestContext testContext, String sdxKey, SdxCloudStorageRequest cloudStorageRequest,
            SdxResizeTestValidator validator, Boolean recoveryTest) {
        SdxDatabaseRequest sdxDatabaseRequest = new SdxDatabaseRequest();
        sdxDatabaseRequest.setAvailabilityType(SdxDatabaseAvailabilityType.NONE);
        return testContext
                .given("telemetry", TelemetryTestDto.class)
                .withLogging()
                .withReportClusterLogs()
                // Create original SDX cluster.
                .given(sdxKey, SdxInternalTestDto.class)
                .withDatabase(sdxDatabaseRequest)
                .withCloudStorage(cloudStorageRequest)
                .withRuntimeVersion(MDL_RESIZE_FIXED_RUNTIME_VERSION)
                .withClusterShape(SdxClusterShape.CUSTOM)
                .withTelemetry("telemetry")
                .when(sdxTestClient.createInternal(), key(sdxKey))
                .await(SdxClusterStatusResponse.RUNNING, key(sdxKey))
                .awaitForHealthyInstances()

                // Initialize validator with info from original cluster.
                .then((tc, testDto, client) -> {
                    validator.setExpectedCrn(sdxUtil.getCrn(testDto, client));
                    validator.setExpectedName(testDto.getName());
                    validator.setExpectedRuntime(sdxUtil.getRuntime(testDto, client));
                    if (recoveryTest) {
                        validator.setExpectedCreationTimestamp(sdxUtil.getCreated(testDto, client));
                    }
                    return testDto;
                })

                // Perform resize and await start of new cluster creation.
                .when(sdxTestClient.resize(), key(sdxKey))
                .await(SdxClusterStatusResponse.STOP_IN_PROGRESS, key(sdxKey).withoutWaitForFlow())
                .await(SdxClusterStatusResponse.STACK_CREATION_IN_PROGRESS, key(sdxKey).withoutWaitForFlow());
    }

    private SdxInternalTestDto performProvisioningFailureAndRecovery(SdxInternalTestDto testDto, String sdxKey) {
        return testDto
                // Perform provisioning failure.
                .awaitForInstancesToExist()
                .awaitForStartingInstances()
                .then((tc, dto, client) -> {
                    List<String> instanceIdsToDelete = sdxUtil.getInstanceIds(dto, client, MASTER.getName());
                    tc.getCloudProvider().getCloudFunctionality().deleteInstances(dto.getName(), instanceIdsToDelete);
                    return testDto;
                })
                .await(SdxClusterStatusResponse.PROVISIONING_FAILED, key(sdxKey).withoutWaitForFlow())

                // Perform resize recovery.
                .when(sdxTestClient.recoverFromResizeInternal(), key(sdxKey))
                .await(SdxClusterStatusResponse.RUNNING, key(sdxKey))
                .awaitForHealthyInstances();
    }
}
