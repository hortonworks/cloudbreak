package com.sequenceiq.it.cloudbreak.testcase.e2e.sdx;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;

import java.util.concurrent.atomic.AtomicReference;

import javax.inject.Inject;

import org.testng.annotations.Ignore;
import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.util.SdxUtil;
import com.sequenceiq.it.cloudbreak.util.spot.UseSpotInstances;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;

@Ignore
public class SdxResizeTests extends PreconditionSdxE2ETest {
    @Inject
    private SdxTestClient sdxTestClient;

    @Inject
    private SdxUtil sdxUtil;

    @Test(dataProvider = TEST_CONTEXT)
    @UseSpotInstances
    @Description(
            given = "there is a running Cloudbreak, and an SDX cluster in available state",
            when = "upgrade called on the SDX cluster",
            then = "SDX upgrade should be successful, the cluster should be up and running"
    )
    public void testSDXResize(TestContext testContext) {
        String sdx = resourcePropertyProvider().getName();
        String resizedSdx = sdx + "-md";
        AtomicReference<String> actualShape = new AtomicReference<>();
        AtomicReference<String> expectedShape = new AtomicReference<>();

        testContext
                .given(sdx, SdxInternalTestDto.class)
                .when(sdxTestClient.createInternal(), key(sdx))
                .await(SdxClusterStatusResponse.RUNNING, key(sdx))
                .awaitForHealthyInstances()
                .then((tc, testDto, client) -> {
                    expectedShape.set(sdxUtil.getShape(testDto, client));
                    return testDto;
                })
                .when(sdxTestClient.resize(), key(sdx))
                .await(SdxClusterStatusResponse.STOP_IN_PROGRESS, key(sdx).withWaitForFlow(Boolean.FALSE))
                .await(SdxClusterStatusResponse.DATALAKE_DETACHED)
                .await(SdxClusterStatusResponse.RUNNING, key(resizedSdx))
                .awaitForHealthyInstances()
                .then((tc, testDto, client) -> {
                    actualShape.set(sdxUtil.getShape(testDto, client));
                    return testDto;
                })
                .then((tc, testDto, client) ->  {
                    if (actualShape.get().equals(expectedShape.get())) {
                        throw new TestFailException("SDX shape did not change after re-sizing");
                    }
                    return testDto;
                })
                .validate();
    }
}
