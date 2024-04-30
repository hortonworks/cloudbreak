package com.sequenceiq.it.cloudbreak.testcase.e2e.sdx;

import java.util.concurrent.atomic.AtomicReference;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.cloud.v4.CommonClusterManagerProperties;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.imagecatalog.ImageCatalogTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxTestDto;
import com.sequenceiq.it.cloudbreak.testcase.e2e.AbstractE2ETest;
import com.sequenceiq.it.cloudbreak.util.CloudFunctionality;
import com.sequenceiq.it.cloudbreak.util.SdxUtil;

public class PreconditionSdxE2ETest extends AbstractE2ETest {

    private static final Logger LOGGER = LoggerFactory.getLogger(PreconditionSdxE2ETest.class);

    @Inject
    private SdxTestClient sdxTestClient;

    @Inject
    private SdxUtil sdxUtil;

    @Inject
    private CommonClusterManagerProperties commonClusterManagerProperties;

    @Override
    protected void setupTest(TestContext testContext) {
        testContext.getCloudProvider().getCloudFunctionality().cloudStorageInitialize();
        createDefaultUser(testContext);
        initializeDefaultBlueprints(testContext);
        createDefaultCredential(testContext);
        initializeAzureMarketplaceTermsPolicy(testContext);
        createEnvironmentWithFreeIpa(testContext);
    }

    protected SdxTestClient sdxTestClient() {
        return sdxTestClient;
    }

    protected SdxUtil sdxUtil() {
        return sdxUtil;
    }

    protected CommonClusterManagerProperties commonClusterManagerProperties() {
        return commonClusterManagerProperties;
    }

    protected String getDefaultSDXBlueprintName() {
        return commonClusterManagerProperties().getInternalSdxBlueprintName();
    }

    protected String getBaseLocation(SdxTestDto testDto) {
        return testDto.getRequest().getCloudStorage().getBaseLocation();
    }

    protected CloudFunctionality getCloudFunctionality(TestContext testContext) {
        return testContext.getCloudProvider().getCloudFunctionality();
    }

    protected String getLatestPrewarmedImageId(TestContext testContext) {
        AtomicReference<String> selectedImageID = new AtomicReference<>();
        testContext
                .given(ImageCatalogTestDto.class)
                .when((tc, dto, client) -> {
                    selectedImageID.set(tc.getCloudProvider().getLatestPreWarmedImageID(tc, dto, client));
                    return dto;
                });
        return selectedImageID.get();
    }
}
