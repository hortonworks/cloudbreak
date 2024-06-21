package com.sequenceiq.it.cloudbreak.testcase.e2e.sdx;

import jakarta.inject.Inject;

import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.util.resize.SdxResizeTestUtil;
import com.sequenceiq.it.cloudbreak.util.spot.UseSpotInstances;

public class SdxResizeTests extends PreconditionSdxE2ETest {

    @Inject
    private SdxResizeTestUtil sdxResizeTestUtil;

    @Test(dataProvider = TEST_CONTEXT)
    @UseSpotInstances
    @Description(
            given = "there is an available environment with a running SDX cluster",
            when = "resize is called on the SDX cluster",
            then = "SDX resize should be successful, the new cluster should be up and running"
    )
    public void testSDXResize(TestContext testContext) {
        sdxResizeTestUtil.runResizeTest(testContext, resourcePropertyProvider().getName(), getCloudStorageRequest(testContext));
    }

}
