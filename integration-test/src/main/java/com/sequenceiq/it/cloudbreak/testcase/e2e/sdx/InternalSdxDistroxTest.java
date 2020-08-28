package com.sequenceiq.it.cloudbreak.testcase.e2e.sdx;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;

import javax.inject.Inject;

import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.client.DistroXTestClient;
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.image.DistroXImageTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.util.spot.UseSpotInstances;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;
import com.sequenceiq.sdx.api.model.SdxDatabaseAvailabilityType;
import com.sequenceiq.sdx.api.model.SdxDatabaseRequest;

public class InternalSdxDistroxTest extends ImageValidatorE2ETest {

    @Inject
    private SdxTestClient sdxTestClient;

    @Inject
    private DistroXTestClient distroXTestClient;

    @Test(dataProvider = TEST_CONTEXT)
    @UseSpotInstances
    @Description(
            given = "a SDX internal request and a DistroX request",
            when = "a SDX internal create request is sent",
            then = "the SDX cluster and the corresponding DistroX cluster is created")
    public void testCreateInternalSdxAndDistrox(TestContext testContext) {
        SdxDatabaseRequest sdxDatabaseRequest = new SdxDatabaseRequest();
        sdxDatabaseRequest.setAvailabilityType(SdxDatabaseAvailabilityType.NONE);
        sdxDatabaseRequest.setCreate(false);
        testContext.given(SdxInternalTestDto.class)
                .withDatabase(sdxDatabaseRequest)
                .withCloudStorage(getCloudStorageRequest(testContext))
                .withTemplate(commonClusterManagerProperties().getInternalSdxBlueprintName())
                .withRuntimeVersion(commonClusterManagerProperties().getRuntimeVersion())
                .withImageCatalogNameOnly(commonCloudProperties().getImageValidation().getSourceCatalogName())
                .when(sdxTestClient.createInternal())
                .awaitForFlow(key(resourcePropertyProvider().getName()))
                .await(SdxClusterStatusResponse.RUNNING)
                .when(sdxTestClient.describeInternal())
                .validate();
        testContext.given(DistroXTestDto.class)
                .withTemplate(commonClusterManagerProperties().getInternalDistroXBlueprintName())
                .withImageSettings(testContext
                        .given(DistroXImageTestDto.class)
                        .withImageCatalog(testContext.get(SdxInternalTestDto.class).getResponse().getStackV4Response().getImage().getCatalogName())
                        .withImageId(testContext.get(SdxInternalTestDto.class).getResponse().getStackV4Response().getImage().getId()))
                .when(distroXTestClient.create())
                .awaitForFlow(key(resourcePropertyProvider().getName()))
                .await(STACK_AVAILABLE)
                .then((context, distrox, client) -> {
                    distrox.getResponse();
                    return distrox;
                })
                .when(distroXTestClient.get())
                .validate();
    }

    @Override
    protected String getImageId(TestContext testContext) {
        return testContext.get(SdxInternalTestDto.class).getResponse().getStackV4Response().getImage().getId();
    }
}
