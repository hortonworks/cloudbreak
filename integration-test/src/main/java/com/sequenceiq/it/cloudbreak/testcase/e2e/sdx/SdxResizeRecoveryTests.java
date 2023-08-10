package com.sequenceiq.it.cloudbreak.testcase.e2e.sdx;

import javax.inject.Inject;

import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.util.resize.SdxResizeTestUtil;
import com.sequenceiq.it.cloudbreak.util.spot.UseSpotInstances;

public class SdxResizeRecoveryTests extends PreconditionSdxE2ETest {
    @Inject
    private SdxResizeTestUtil sdxResizeTestUtil;

    @Test(dataProvider = TEST_CONTEXT)
    @UseSpotInstances
    @Description(
            given = "there is an available environment with a running SDX cluster",
            when = "resize is performed on the SDX cluster but fails during provisioning",
            then = "recovery should be available and successful when run, the original cluster should be up and running"
    )
    public void testSdxResizeRecoveryFromProvisioningFailure(TestContext testContext) {
        sdxResizeTestUtil.runResizeRecoveryFromProvisioningFailureMediumDutyTest(
                testContext, resourcePropertyProvider().getName(), getCloudStorageRequest(testContext)
        );
    }

    @Test(dataProvider = TEST_CONTEXT)
    @UseSpotInstances
    @Description(
            given = "there is an available environment with a running SDX cluster",
            when = "resize is performed on the SDX cluster but fails during provisioning",
            then = "recovery should be available and successful when run, the original cluster should be up and running"
    )
    public void testSdxResizeRecoveryFromProvisioningFailureEDL(TestContext testContext) {
        sdxResizeTestUtil.runResizeTestEDLWithRecovery(testContext, resourcePropertyProvider().getName(), getCloudStorageRequest(testContext)
        );
    }
}
