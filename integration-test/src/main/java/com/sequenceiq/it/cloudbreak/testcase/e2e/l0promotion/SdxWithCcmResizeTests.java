package com.sequenceiq.it.cloudbreak.testcase.e2e.l0promotion;

import jakarta.inject.Inject;

import org.testng.annotations.Test;

import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.it.cloudbreak.client.EnvironmentTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.testcase.e2e.sdx.PreconditionSdxE2ETest;
import com.sequenceiq.it.cloudbreak.util.EnvironmentUtil;
import com.sequenceiq.it.cloudbreak.util.resize.SdxResizeTestUtil;
import com.sequenceiq.it.cloudbreak.util.spot.UseSpotInstances;

public class SdxWithCcmResizeTests extends PreconditionSdxE2ETest {
    @Inject
    private EnvironmentTestClient environmentTestClient;

    @Inject
    private EnvironmentUtil environmentUtil;

    @Inject
    private SdxResizeTestUtil sdxResizeTestUtil;

    @Override
    protected void initiateEnvironmentCreation(TestContext testContext) {
        environmentUtil.createEnvironmentWithDefinedCcm(testContext, Tunnel.CCMV2_JUMPGATE)
                .when(environmentTestClient.create())
                .awaitForCreationFlow()
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT)
    @UseSpotInstances
    @Description(
            given = "there is an available environment with a running SDX cluster connected via CCMv2 Jumpgate",
            when = "resize is called on the SDX cluster",
            then = "SDX resize should be successful, the new cluster should be up and running"
    )
    public void testCcmSDXResize(TestContext testContext) {
        sdxResizeTestUtil.runResizeTest(
                testContext, resourcePropertyProvider().getName(), getCloudStorageRequest(testContext)
        );
    }
}
