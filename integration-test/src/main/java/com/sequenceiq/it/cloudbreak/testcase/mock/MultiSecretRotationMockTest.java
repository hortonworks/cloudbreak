package com.sequenceiq.it.cloudbreak.testcase.mock;

import java.util.Set;

import javax.inject.Inject;

import org.testng.ITestContext;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.rotation.CloudbreakSecretType;
import com.sequenceiq.it.cloudbreak.client.DistroXTestClient;
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.sdx.rotation.DatalakeSecretType;

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
                .when(sdxTestClient.rotateSecret(Set.of(DatalakeSecretType.DATALAKE_DEMO_SECRET)))
                .awaitForFlow()
                .given(DistroXTestDto.class)
                .when(distroXTestClient.rotateSecret(Set.of(CloudbreakSecretType.DATAHUB_DEMO_SECRET)))
                .awaitForFlow()
                .given(SdxInternalTestDto.class)
                .when(sdxTestClient.rotateSecret(Set.of(DatalakeSecretType.DATALAKE_DEMO_SECRET)))
                .awaitForFlow()
                .validate();
    }
}
