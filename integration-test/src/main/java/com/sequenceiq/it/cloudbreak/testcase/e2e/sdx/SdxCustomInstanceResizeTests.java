package com.sequenceiq.it.cloudbreak.testcase.e2e.sdx;

import jakarta.inject.Inject;

import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.util.resize.SdxResizeTestUtil;
import com.sequenceiq.it.cloudbreak.util.spot.UseSpotInstances;

public class SdxCustomInstanceResizeTests extends PreconditionSdxE2ETest {
    private static final String RUNTIME_VERSION = "7.2.17";

    @Inject
    private SdxResizeTestUtil sdxResizeTestUtil;

    @Test(dataProvider = TEST_CONTEXT)
    @UseSpotInstances
    @Description(
            given = "there is an available environment with a running SDX cluster",
            when = "resize is called on the SDX cluster with custom instances",
            then = "SDX resize should be successful, the new cluster running with custom instances from previous SDX cluster"
    )
    public void testSDXCustomResize(TestContext testContext) {
        sdxResizeTestUtil.runCustomInstancesResizeTest(testContext, resourcePropertyProvider().getName(), getCloudStorageRequest(testContext));
    }
}
