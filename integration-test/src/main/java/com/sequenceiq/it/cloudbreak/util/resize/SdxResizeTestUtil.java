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
import com.sequenceiq.it.cloudbreak.testcase.e2e.sdx.PreconditionSdxE2ETest;
import com.sequenceiq.it.cloudbreak.util.SdxUtil;
import com.sequenceiq.sdx.api.model.SdxCloudStorageRequest;
import com.sequenceiq.sdx.api.model.SdxClusterResizeRequest;
import com.sequenceiq.sdx.api.model.SdxClusterShape;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;
import com.sequenceiq.sdx.api.model.SdxInstanceGroupDiskRequest;
import com.sequenceiq.sdx.api.model.SdxInstanceGroupRequest;

@Component
public class SdxResizeTestUtil extends PreconditionSdxE2ETest  {

    private static final String MDL_RESIZE_FIXED_RUNTIME_VERSION = "7.2.17";

    @Inject
    private SdxTestClient sdxTestClient;

    @Inject
    private SdxUtil sdxUtil;

    public void runResizeTest(TestContext testContext, String sdxKey, SdxCloudStorageRequest cloudStorageRequest) {
        SdxResizeTestValidator resizeTestValidator = new SdxResizeTestValidator(SdxClusterShape.ENTERPRISE);

        performInitialResizeSteps(testContext, sdxKey, cloudStorageRequest, resizeTestValidator, false, false)
                .await(SdxClusterStatusResponse.RUNNING, key(sdxKey))
                .awaitForHealthyInstances()

                // Ensure new cluster is of the right shape and has carried over the necessary info from the original cluster.
                .then((tc, dto, client) -> resizeTestValidator.validateResizedCluster(dto))
                .validate();
    }

    public void runResizeRecoveryFromProvisioningFailureTest(TestContext testContext, String sdxKey, SdxCloudStorageRequest cloudStorageRequest) {
        SdxResizeTestValidator resizeTestValidator = new SdxResizeTestValidator(SdxClusterShape.CUSTOM);

        performInitialResizeSteps(testContext, sdxKey, cloudStorageRequest, resizeTestValidator, true, false)
                .then((tc, testDto, client) -> performProvisioningFailureAndRecovery(testDto, sdxKey))

                // Ensure current cluster is identical to original cluster.
                .then((tc, dto, client) -> resizeTestValidator.validateRecoveredCluster(dto))
                .validate();
    }

    public void runCustomInstancesResizeTest(TestContext testContext, String sdxKey, SdxCloudStorageRequest cloudStorageRequest) {
        SdxResizeTestValidator resizeTestValidator = new SdxResizeTestValidator(SdxClusterShape.ENTERPRISE);

        performInitialResizeSteps(testContext, sdxKey, cloudStorageRequest, resizeTestValidator, false, true)
                .await(SdxClusterStatusResponse.RUNNING, key(sdxKey))
                .awaitForHealthyInstances()
                .then((tc, dto, client) -> resizeTestValidator.validateResizedCluster(dto))
                .validate();
    }

    private SdxInternalTestDto performInitialResizeSteps(TestContext testContext, String sdxKey, SdxCloudStorageRequest cloudStorageRequest,
            SdxResizeTestValidator validator, Boolean recoveryTest, Boolean useCustomInstances) {
        return testContext
                .given("telemetry", TelemetryTestDto.class)
                .withLogging()
                .withReportClusterLogs()
                .given(sdxKey, SdxInternalTestDto.class)
                .withCloudStorage(cloudStorageRequest)
                .withRuntimeVersion(MDL_RESIZE_FIXED_RUNTIME_VERSION)
                .withClusterShape(SdxClusterShape.CUSTOM)
                .withTelemetry("telemetry")
                .when(sdxTestClient.createInternal(), key(sdxKey))
                .await(SdxClusterStatusResponse.RUNNING, key(sdxKey))
                .awaitForHealthyInstances()
                .then((tc, testDto, client) -> {
                    validator.setExpectedCrn(sdxUtil.getCrn(testDto, client));
                    validator.setExpectedName(testDto.getName());
                    validator.setExpectedRuntime(sdxUtil.getRuntime(testDto, client));
                    if (recoveryTest) {
                        validator.setExpectedCreationTimestamp(sdxUtil.getCreated(testDto, client));
                    }
                    return testDto;
                })
                .then((tc, testDto, client) -> {
                    SdxClusterResizeRequest sdxClusterResizeRequest = testDto.getSdxResizeRequest();
                    if (useCustomInstances) {
                        populateSdxClusterResizeRequestWithCustomInstances(sdxClusterResizeRequest);
                        validator.setExpectedCustomInstanceGroups(sdxClusterResizeRequest.getCustomInstanceGroups());
                        validator.setExpectedSdxInstanceGroupDiskRequest(sdxClusterResizeRequest.getCustomInstanceGroupDiskSize());
                    }
                    sdxClusterResizeRequest.setSkipValidation(true);

                    return testDto;
                })
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

    private void populateSdxClusterResizeRequestWithCustomInstances(SdxClusterResizeRequest sdxClusterResizeRequest) {
        SdxInstanceGroupRequest sdxInstanceGroupRequest = new SdxInstanceGroupRequest();
        sdxInstanceGroupRequest.setName("master");
        sdxInstanceGroupRequest.setInstanceType("m5.4xlarge");
        SdxInstanceGroupDiskRequest sdxInstanceGroupDiskRequest = new SdxInstanceGroupDiskRequest();
        sdxInstanceGroupDiskRequest.setName("master");
        sdxInstanceGroupDiskRequest.setInstanceDiskSize(300);

        sdxClusterResizeRequest.setCustomInstanceGroups(List.of(sdxInstanceGroupRequest));
        sdxClusterResizeRequest.setCustomInstanceGroupDiskSize(List.of(sdxInstanceGroupDiskRequest));
    }

    public SdxInternalTestDto givenProvisionEnvironmentAndDatalake(TestContext testContext, String sdxKey, String runtimeVersion,
            SdxClusterShape clusterShape, SdxResizeTestValidator validator) {
        SdxCloudStorageRequest cloudStorageRequest = getCloudStorageRequest(testContext);

        return testContext
                .given("telemetry", TelemetryTestDto.class)
                .withLogging()
                .withReportClusterLogs()
                .given(sdxKey, SdxInternalTestDto.class)
                .withCloudStorage(cloudStorageRequest)
                .withRuntimeVersion(runtimeVersion)
                .withClusterShape(clusterShape)
                .withTelemetry("telemetry")
                .when(sdxTestClient.createInternal(), key(sdxKey))
                .await(SdxClusterStatusResponse.RUNNING, key(sdxKey))
                .awaitForHealthyInstances()
                .then((tc, testDto, client) -> {
                    validator.setExpectedCrn(sdxUtil.getCrn(testDto, client));
                    validator.setExpectedName(testDto.getName());
                    validator.setExpectedRuntime(sdxUtil.getRuntime(testDto, client));
                    return testDto;
                });
    }
}
