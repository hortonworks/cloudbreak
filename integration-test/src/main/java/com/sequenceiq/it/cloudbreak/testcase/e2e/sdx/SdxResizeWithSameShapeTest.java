package com.sequenceiq.it.cloudbreak.testcase.e2e.sdx;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;

import jakarta.inject.Inject;

import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.cloud.v4.CommonClusterManagerProperties;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.util.resize.SdxResizeTestUtil;
import com.sequenceiq.it.cloudbreak.util.resize.SdxResizeTestValidator;
import com.sequenceiq.it.cloudbreak.util.spot.UseSpotInstances;
import com.sequenceiq.sdx.api.model.SdxClusterResizeRequest;
import com.sequenceiq.sdx.api.model.SdxClusterShape;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;

public class SdxResizeWithSameShapeTest extends PreconditionSdxE2ETest {
    @Inject
    private CommonClusterManagerProperties commonClusterManagerProperties;

    @Inject
    private SdxTestClient sdxTestClient;

    @Inject
    private SdxResizeTestUtil sdxResizeTestUtil;

    @Inject
    private SdxResizeTestValidator sdxResizeTestValidator;

    @Test(dataProvider = TEST_CONTEXT)
    @UseSpotInstances
    @Description(
            given = "there is an available environment with a running SDX cluster (ENT shape, single-az)",
            when = "resize is called on the SDX cluster (target shape: ENTERPRISE multi-az )",
            then = "SDX resize should be successful, the new cluster should be up and running"
    )
    public void testSdxSameShapeResize(TestContext testContext) {
        String sdxKey = resourcePropertyProvider().getName();
        String runtimeVersion = commonClusterManagerProperties.getRuntimeVersion();

        sdxResizeTestUtil
                .givenProvisionEnvironmentAndDatalake(testContext, sdxKey, runtimeVersion, SdxClusterShape.ENTERPRISE, sdxResizeTestValidator)
                .then((tc, testDto, client) -> {
                    SdxClusterResizeRequest sdxClusterResizeRequest = testDto.getSdxResizeRequest();
                    sdxClusterResizeRequest.setClusterShape(SdxClusterShape.ENTERPRISE);
                    sdxClusterResizeRequest.setEnableMultiAz(true);
                    sdxResizeTestValidator.setExpectedMultiAzDatalake(true);
                    sdxResizeTestValidator.setExpectedSameShapeResize(true);
                    sdxClusterResizeRequest.setSkipValidation(true);
                    return testDto;
                })
                .when(sdxTestClient.resize(), key(sdxKey))
                .await(SdxClusterStatusResponse.STOP_IN_PROGRESS, key(sdxKey).withoutWaitForFlow())
                .await(SdxClusterStatusResponse.STACK_CREATION_IN_PROGRESS, key(sdxKey).withoutWaitForFlow())
                .await(SdxClusterStatusResponse.RUNNING, key(sdxKey))
                .awaitForHealthyInstances()
                // Ensure new cluster is of the right shape and has carried over the necessary info from the original cluster.
                .then((tc, dto, client) -> sdxResizeTestValidator.validateResizedCluster(dto, tc))
                .validate();
    }
}
