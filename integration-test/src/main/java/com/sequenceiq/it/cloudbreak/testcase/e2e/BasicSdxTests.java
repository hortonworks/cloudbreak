package com.sequenceiq.it.cloudbreak.testcase.e2e;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;

import java.io.IOException;

import javax.inject.Inject;

import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxTestDto;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;

public class BasicSdxTests extends AbstractE2ETest {

    protected static final SdxClusterStatusResponse SDX_RUNNING = SdxClusterStatusResponse.RUNNING;

    protected static final SdxClusterStatusResponse SDX_DELETED = SdxClusterStatusResponse.DELETED;

    @Inject
    private SdxTestClient sdxTestClient;

    @Override
    protected void setupTest(TestContext testContext) {
        createDefaultUser(testContext);
        createDefaultCredential(testContext);
        createEnvironmentWithNetworkAndFreeIPA(testContext);
        initializeDefaultBlueprints(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT)
    @Description(
            given = "there is a running Cloudbreak",
            when = "a valid SDX create request is sent",
            then = "SDX should be available AND deletable"
    )
    public void testSDXCanBeCreatedThenDeletedSuccessfully(TestContext testContext) throws IOException {
        String sdx = resourcePropertyProvider().getName();

        testContext
                .given(sdx, SdxTestDto.class)
                .when(sdxTestClient.create(), key(sdx))
                .await(SDX_RUNNING)
                .then((tc, testDto, client) -> sdxTestClient.delete().action(tc, testDto, client))
                .await(SDX_DELETED)
                .validate();
    }

    protected SdxTestClient sdxTestClient() {
        return sdxTestClient;
    }
}
