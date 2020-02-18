package com.sequenceiq.it.cloudbreak.testcase.e2e.sdx;

import javax.inject.Inject;

import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.client.DistroXTestClient;
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.image.DistroXImageTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.testcase.e2e.ImageValidatorE2ETest;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;
import com.sequenceiq.sdx.api.model.SdxDatabaseRequest;

public class InternalSdxDistroxTests extends ImageValidatorE2ETest {

    @Inject
    private SdxTestClient sdxTestClient;

    @Inject
    private DistroXTestClient distroXTestClient;

    @Override
    protected void setupTest(TestContext testContext) {
        testContext.getCloudProvider().getCloudFunctionality().cloudStorageInitialize();
        createDefaultUser(testContext);
        createDefaultCredential(testContext);
        createEnvironmentWithNetworkAndFreeIPA(testContext);
        initializeDefaultBlueprints(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT)
    @Description(
            given = "a SDX internal request and a DistroX request",
            when = "a SDX internal create request is sent",
            then = "the SDX cluster and the corresponding DistroX cluster is created")
    public void testCreateInternalSdxAndDistrox(TestContext testContext) {
        SdxDatabaseRequest sdxDatabaseRequest = new SdxDatabaseRequest();
        sdxDatabaseRequest.setCreate(false);
        testContext.given(SdxInternalTestDto.class)
                .withDatabase(sdxDatabaseRequest)
                .withTemplate(sdxTemplateName)
                .withRuntimeVersion(runtimeVersion)
                .when(sdxTestClient.createInternal())
                .await(SdxClusterStatusResponse.RUNNING)
                .when(sdxTestClient.describeInternal())
                .validate();
        testContext.given(DistroXTestDto.class)
                .withTemplate(distroxTemplateName)
                .withImageSettings(testContext
                        .given(DistroXImageTestDto.class)
                        .withImageCatalog(testContext.get(SdxInternalTestDto.class).getResponse().getStackV4Response().getImage().getCatalogName())
                        .withImageId(testContext.get(SdxInternalTestDto.class).getResponse().getStackV4Response().getImage().getId()))
                .when(distroXTestClient.create())
                .await(STACK_AVAILABLE)
                .when(distroXTestClient.get())
                .validate();
    }

    @Override
    protected String getImageId(TestContext testContext) {
        return testContext.get(SdxInternalTestDto.class).getResponse().getStackV4Response().getImage().getId();
    }
}
