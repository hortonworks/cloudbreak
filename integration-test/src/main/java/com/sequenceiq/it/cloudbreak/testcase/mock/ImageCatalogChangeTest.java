package com.sequenceiq.it.cloudbreak.testcase.mock;

import javax.inject.Inject;

import org.jetbrains.annotations.NotNull;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.image.StackImageV4Response;
import com.sequenceiq.it.cloudbreak.client.DistroXTestClient;
import com.sequenceiq.it.cloudbreak.client.FreeIpaTestClient;
import com.sequenceiq.it.cloudbreak.client.ImageCatalogTestClient;
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.image.DistroXChangeImageCatalogTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeipaChangeImageCatalogTestDto;
import com.sequenceiq.it.cloudbreak.dto.imagecatalog.ImageCatalogTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxChangeImageCatalogTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;

public class ImageCatalogChangeTest extends AbstractMockTest {

    @Inject
    private FreeIpaTestClient freeIpaTestClient;

    @Inject
    private SdxTestClient sdxTestClient;

    @Inject
    private DistroXTestClient distroXClient;

    @Inject
    private ImageCatalogTestClient imageCatalogTestClient;

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "a running Freeipa/Datalake/Datahub",
            when = "calling change image catalog",
            then = "it should have new image catalog")
    public void testFreeipaImageCatalogChange(MockedTestContext testContext) {
        // Freeipa
        final String newImageCatalog = testContext.given(FreeIpaTestDto.class).getResponse().getImage().getCatalog() + "&changed=true";
        testContext.given(FreeipaChangeImageCatalogTestDto.class)
                    .withImageCatalog(newImageCatalog)
                .when(freeIpaTestClient.changeImageCatalog())
                .given(FreeIpaTestDto.class)
                .when(freeIpaTestClient.describe())
                .then((testContext1, testDto, client) -> {
                    final String actualCatalog = testDto.getResponse().getImage().getCatalog();
                    if (!newImageCatalog.equals(actualCatalog)) {
                        throw new TestFailException(String.format("Image catalog of Freeipa was not changed. Catalog : %s", actualCatalog));
                    }
                    return testDto;
                });

        // Datalake
        testContext.given(SdxInternalTestDto.class)
                .when(sdxTestClient.createInternal())
                .await(SdxClusterStatusResponse.RUNNING);

        final StackImageV4Response sdxImage = testContext.given(SdxInternalTestDto.class).getResponse().getStackV4Response().getImage();
        final String sdxNewImageCatalogName = createNewImageCatalog(testContext, sdxImage);

        testContext.given(SdxChangeImageCatalogTestDto.class)
                    .withImageCatalog(sdxNewImageCatalogName)
                .when(sdxTestClient.changeImageCatalog())
                .given(SdxInternalTestDto.class)
                .when(sdxTestClient.describeInternal())
                .then((testContext1, testDto, client) -> {
                    final String actualCatalog = testDto.getResponse().getStackV4Response().getImage().getCatalogName();
                    if (!sdxNewImageCatalogName.equals(actualCatalog)) {
                        throw new TestFailException(String.format("Image catalog of Datalake was not changed. Catalog : %s", actualCatalog));
                    }
                    return testDto;
                });

        // Datahub
        testContext.given(DistroXTestDto.class)
                .when(distroXClient.create())
                .await(STACK_AVAILABLE);

        final StackImageV4Response distroXImage = testContext.given(DistroXTestDto.class).getResponse().getImage();
        final String distroXNewImageCatalogName = createNewImageCatalog(testContext, distroXImage);

        testContext.given(DistroXChangeImageCatalogTestDto.class)
                    .withImageCatalog(distroXNewImageCatalogName)
                .when(distroXClient.changeImageCatalog())
                .given(DistroXTestDto.class)
                .when(distroXClient.get())
                .then((testContext1, testDto, client) -> {
                    final String actualCatalog = testDto.getResponse().getImage().getCatalogName();
                    if (!distroXNewImageCatalogName.equals(actualCatalog)) {
                        throw new TestFailException(String.format("Image catalog of Datahub was not changed. Catalog : %s", actualCatalog));
                    }
                    return testDto;
                })
                .validate();
    }

    @NotNull
    private String createNewImageCatalog(MockedTestContext testContext, StackImageV4Response image) {
        final String newImageCatalogName = image.getCatalogName() + "-changed";
        final String newImageCatalogUrl = image.getCatalogUrl() + "&changed=true";
        testContext.given(ImageCatalogTestDto.class)
                .withName(newImageCatalogName)
                .withUrl(newImageCatalogUrl)
                .when(imageCatalogTestClient.createV4());
        return newImageCatalogName;
    }
}
