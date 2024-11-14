package com.sequenceiq.it.cloudbreak.testcase.e2e.distrox;


import jakarta.inject.Inject;

import org.testng.ITestContext;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.it.cloudbreak.client.DistroXTestClient;
import com.sequenceiq.it.cloudbreak.client.FreeIpaTestClient;
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.testcase.e2e.AbstractE2ETest;

public class DistroXImdsV2Tests extends AbstractE2ETest {

    @Inject
    private DistroXTestClient distroXTestClient;

    @Inject
    private SdxTestClient sdxTestClient;

    @Inject
    private FreeIpaTestClient freeIpaTestClient;

    @Override
    protected void setupTest(TestContext testContext) {
        assertSupportedCloudPlatform(CloudPlatform.AWS);
        testContext.getCloudProvider().getCloudFunctionality().cloudStorageInitialize();
        createDefaultUser(testContext);
        createDefaultCredential(testContext);
        initializeDefaultBlueprints(testContext);
        createDefaultDatahubWithAutoTlsAndExternalDb(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT)
    @Description(
            given = "there is a running default Distrox cluster",
            when = "IMDS updated",
            then = "update should be successful and clusters should be available")
    public void testCMSharedDbMultiSecretRotationAndImdsUpdate(TestContext testContext, ITestContext iTestContext) {
        testContext
                .given(FreeIpaTestDto.class)
                .when(freeIpaTestClient.describe())
                .when(freeIpaTestClient.instanceMetadataUpdate())
                .awaitForFlow()
                .given(SdxInternalTestDto.class)
                .when(sdxTestClient.describeInternal())
                .when(sdxTestClient.instanceMetadataUpdate())
                .awaitForFlow()
                .given(DistroXTestDto.class)
                .when(distroXTestClient.get())
                .when(distroXTestClient.instanceMetadataUpdate())
                .awaitForFlow()
                .validate();
    }
}