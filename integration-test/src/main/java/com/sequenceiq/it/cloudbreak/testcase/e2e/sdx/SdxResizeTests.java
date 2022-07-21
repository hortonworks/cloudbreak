package com.sequenceiq.it.cloudbreak.testcase.e2e.sdx;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;

import javax.inject.Inject;

import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.util.resize.SdxResizeTestValidator;
import com.sequenceiq.it.cloudbreak.util.SdxUtil;
import com.sequenceiq.it.cloudbreak.util.spot.UseSpotInstances;
import com.sequenceiq.sdx.api.model.SdxClusterShape;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;
import com.sequenceiq.sdx.api.model.SdxDatabaseAvailabilityType;
import com.sequenceiq.sdx.api.model.SdxDatabaseRequest;

public class SdxResizeTests extends PreconditionSdxE2ETest {
    @Inject
    private SdxTestClient sdxTestClient;

    @Inject
    private SdxUtil sdxUtil;

    @Test(dataProvider = TEST_CONTEXT)
    @UseSpotInstances
    @Description(
            given = "there is a running Cloudbreak, and an SDX cluster in available state",
            when = "resize called on the SDX cluster",
            then = "SDX resize should be successful, the cluster should be up and running"
    )
    public void testSDXResize(TestContext testContext) {
        String sdx = resourcePropertyProvider().getName();
        SdxResizeTestValidator resizeTestValidator = new SdxResizeTestValidator(SdxClusterShape.MEDIUM_DUTY_HA);
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
                    return testDto;
                })
                .when(sdxTestClient.resize(), key(sdx))
                .await(SdxClusterStatusResponse.STOP_IN_PROGRESS, key(sdx).withWaitForFlow(Boolean.FALSE))
                .await(SdxClusterStatusResponse.STACK_CREATION_IN_PROGRESS, key(sdx).withWaitForFlow(Boolean.FALSE))
                .await(SdxClusterStatusResponse.RUNNING, key(sdx))
                .awaitForHealthyInstances()
                .then((tc, dto, client) -> resizeTestValidator.validateResizedCluster(dto))
                .validate();
    }
}
