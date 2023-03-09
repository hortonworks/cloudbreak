package com.sequenceiq.it.cloudbreak.testcase.e2e.l0promotion;

import javax.inject.Inject;

import org.testng.annotations.Test;

import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus;
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
        environmentUtil.createCCMv1Environment(testContext)
                    .withFreeIpaImage(commonCloudProperties().getImageValidation().getFreeIpaImageCatalog(),
                        commonCloudProperties().getImageValidation().getFreeIpaImageUuid())
                .when(environmentTestClient.create())
                .await(EnvironmentStatus.AVAILABLE)
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT)
    @UseSpotInstances
    @Description(
            given = "there is an available environment with a running SDX cluster connected via CCMv1",
            when = "resize is called on the SDX cluster",
            then = "SDX resize should be successful, the new cluster should be up and running"
    )
    public void testCcmSDXResize(TestContext testContext) {
        sdxResizeTestUtil.runResizeTest(
                testContext, resourcePropertyProvider().getName(), getCloudStorageRequest(testContext)
        );
    }
}
