package com.sequenceiq.it.cloudbreak.testcase.e2e.sdx;

import static com.sequenceiq.it.cloudbreak.cloud.HostGroupType.MASTER;
import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;

import java.util.List;

import javax.inject.Inject;

import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.SdxClient;
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.util.resize.SdxResizeTestValidator;
import com.sequenceiq.it.cloudbreak.util.SdxUtil;
import com.sequenceiq.it.cloudbreak.util.spot.UseSpotInstances;
import com.sequenceiq.sdx.api.model.SdxClusterShape;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;
import com.sequenceiq.sdx.api.model.SdxDatabaseAvailabilityType;
import com.sequenceiq.sdx.api.model.SdxDatabaseRequest;

public class SdxResizeRecoveryTests extends PreconditionSdxE2ETest {
    private static final String RESTORE_FAILURE_RESPONSE = "Datalake restore failed";

    @Inject
    private SdxTestClient sdxTestClient;

    @Inject
    private SdxUtil sdxUtil;

    @Test(dataProvider = TEST_CONTEXT)
    @UseSpotInstances
    @Description(
            given = "there is a running Cloudbreak, and an SDX cluster in available state",
            when = "resize is performed on the SDX cluster but fails during restore",
            then = "recovery should be available and successful when run, the original cluster should be up and running"
    )
    public void testSdxResizeRecoveryFromRestoreFailure(TestContext testContext) {
        String sdx = resourcePropertyProvider().getName();
        SdxResizeTestValidator resizeTestValidator = new SdxResizeTestValidator(SdxClusterShape.CUSTOM);
        SdxDatabaseRequest sdxDatabaseRequest = new SdxDatabaseRequest();
        sdxDatabaseRequest.setAvailabilityType(SdxDatabaseAvailabilityType.NONE);
        testContext
                .given(sdx, SdxInternalTestDto.class)
                .withDatabase(sdxDatabaseRequest)
                .withCloudStorage(getCloudStorageRequest(testContext))
                .withClusterShape(SdxClusterShape.CUSTOM)
                .when(sdxTestClient.createInternal(), key(sdx))
                .await(SdxClusterStatusResponse.RUNNING, key(sdx))
                .awaitForHealthyInstances()
                .then((tc, testDto, client) -> {
                    resizeTestValidator.setExpectedCrn(sdxUtil.getCrn(testDto, client));
                    resizeTestValidator.setExpectedName(testDto.getName());
                    resizeTestValidator.setExpectedRuntime(sdxUtil.getRuntime(testDto, client));
                    resizeTestValidator.setExpectedCreationTimestamp(sdxUtil.getCreated(testDto, client));
                    return testDto;
                })
                .when(sdxTestClient.resize(), key(sdx))
                .await(SdxClusterStatusResponse.DATALAKE_BACKUP_INPROGRESS, key(sdx).withoutWaitForFlow())
                .await(SdxClusterStatusResponse.STOP_IN_PROGRESS, key(sdx).withoutWaitForFlow())
                .await(SdxClusterStatusResponse.STACK_CREATION_IN_PROGRESS, key(sdx).withoutWaitForFlow())
                .await(SdxClusterStatusResponse.RUNNING, key(sdx).withoutWaitForFlow())
                .then((tc, testDto, client) -> deleteMasterNode(testDto, client, tc))
                .await(SdxClusterStatusResponse.RUNNING, key(sdx).withoutWaitForFlow())
                .then((tc, testDto, client) -> validateRestoreFailure(testDto, client))
                .when(sdxTestClient.recoverFromResizeInternal(), key(sdx))
                .await(SdxClusterStatusResponse.RUNNING, key(sdx))
                .awaitForHealthyInstances()
                .then((tc, dto, client) -> resizeTestValidator.validateRecoveredCluster(dto))
                .validate();
    }

    private SdxInternalTestDto deleteMasterNode(SdxInternalTestDto testDto, SdxClient client, TestContext testContext) {
        List<String> instanceIdsToDelete = sdxUtil.getInstanceIds(testDto, client, MASTER.getName());
        getCloudFunctionality(testContext).deleteInstances(testDto.getName(), instanceIdsToDelete);
        return testDto;
    }

    private SdxInternalTestDto validateRestoreFailure(SdxInternalTestDto testDto, SdxClient client) {
        String statusReason = sdxUtil.getStatusReason(testDto, client);
        if (!statusReason.contains(RESTORE_FAILURE_RESPONSE)) {
            throw new TestFailException(
                    "SDX cluster being resized should have had a restore failure! Instead status reason is: " + statusReason
            );
        }
        return testDto;
    }
}
