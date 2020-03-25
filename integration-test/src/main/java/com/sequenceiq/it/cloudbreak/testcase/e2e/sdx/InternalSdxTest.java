package com.sequenceiq.it.cloudbreak.testcase.e2e.sdx;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;

import javax.inject.Inject;

import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.util.wait.WaitUtil;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;
import com.sequenceiq.sdx.api.model.SdxDatabaseRequest;

public class InternalSdxTest extends PreconditionSdxE2ETest {

    @Inject
    private SdxTestClient sdxTestClient;

    @Inject
    private WaitUtil waitUtil;

    @Test(dataProvider = TEST_CONTEXT)
    @Description(
            given = "an SDX internal request",
            when = "a SDX internal create request is sent",
            then = "SDX is created")
    public void testCreateInternalSdx(TestContext testContext) {
        SdxDatabaseRequest sdxDatabaseRequest = new SdxDatabaseRequest();
        sdxDatabaseRequest.setCreate(false);
        testContext.given(SdxInternalTestDto.class).withDatabase(sdxDatabaseRequest)
                .when(sdxTestClient.createInternal())
                .awaitForFlow(key(resourcePropertyProvider().getName()))
                .await(SdxClusterStatusResponse.RUNNING)
                .then((tc, testDto, client) -> {
                    return waitUtil.waitForSdxInstancesStatus(testDto, client, getSdxInstancesHealthyState());
                })
                .when(sdxTestClient.describeInternal())
                .validate();
    }
}
