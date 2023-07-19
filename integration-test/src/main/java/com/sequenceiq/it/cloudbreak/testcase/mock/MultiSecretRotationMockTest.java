package com.sequenceiq.it.cloudbreak.testcase.mock;

import static com.sequenceiq.sdx.rotation.DatalakeMultiSecretType.DEMO_MULTI_SECRET;

import javax.inject.Inject;

import org.testng.ITestContext;
import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.client.DistroXTestClient;
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;

public class MultiSecretRotationMockTest extends AbstractMockTest {

    @Inject
    private SdxTestClient sdxTestClient;

    @Inject
    private DistroXTestClient distroXTestClient;

    @Override
    protected void setupTest(TestContext testContext) {
        createDefaultUser(testContext);
        createDefaultCredential(testContext);
        createDefaultImageCatalog(testContext);
        initializeDefaultBlueprints(testContext);
        createDefaultDatahub(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running default Distrox cluster",
            when = "multi secrets are getting rotated",
            then = "rotation should be successful and cluster should be available")
    public void testMultiSecretRotation(TestContext testContext, ITestContext iTestContext) {
        testContext
                .given(SdxInternalTestDto.class)
                .when(sdxTestClient.rotateMultiSecret(DEMO_MULTI_SECRET))
                .awaitForFlow()
                .given(DistroXTestDto.class)
                .when(distroXTestClient.rotateMultiSecret(DEMO_MULTI_SECRET))
                .awaitForFlow()
                .given(SdxInternalTestDto.class)
                .when(sdxTestClient.rotateMultiSecret(DEMO_MULTI_SECRET))
                .awaitForFlow()
                .validate();
    }
}
