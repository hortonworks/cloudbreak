package com.sequenceiq.it.cloudbreak.testcase.mock;

import jakarta.inject.Inject;

import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.client.FreeIpaTestClient;
import com.sequenceiq.it.cloudbreak.client.ImageCatalogTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeipaChangeImageCatalogTestDto;
import com.sequenceiq.it.cloudbreak.dto.imagecatalog.ImageCatalogTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;

public class CustomImageCatalogChangeTest extends AbstractMockTest {

    private static final String NAMED_CATALOG = "named-freeipa-catalog";

    @Inject
    private FreeIpaTestClient freeIpaTestClient;

    @Inject
    private ImageCatalogTestClient imageCatalogTestClient;

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "a running FreeIPA with a URL-based image catalog",
            when = "changing image catalog to a named catalog (routes via CoreImageProvider) and back",
            then = "the image should resolve correctly in both directions")
    public void testFreeipaChangeImageCatalogToNamedAndBack(MockedTestContext testContext) {
        String originalCatalogUrl = testContext.given(FreeIpaTestDto.class).getResponse().getImage().getCatalog();
        String originalImageId = testContext.given(FreeIpaTestDto.class).getResponse().getImage().getId();

        createNamedFreeipaImageCatalog(testContext, originalCatalogUrl);

        testContext.given(FreeipaChangeImageCatalogTestDto.class)
                    .withImageCatalog(NAMED_CATALOG)
                .when(freeIpaTestClient.changeImageCatalog())
                .given(FreeIpaTestDto.class)
                .when(freeIpaTestClient.describe())
                .then((tc, testDto, client) -> {
                    String imageId = testDto.getResponse().getImage().getId();
                    if (!originalImageId.equals(imageId)) {
                        throw new TestFailException(
                                String.format("Image ID should remain '%s' after switching to named catalog with same images, but is: %s",
                                        originalImageId, imageId));
                    }
                    return testDto;
                });

        testContext.given(FreeipaChangeImageCatalogTestDto.class)
                    .withImageCatalog(originalCatalogUrl)
                .when(freeIpaTestClient.changeImageCatalog())
                .given(FreeIpaTestDto.class)
                .when(freeIpaTestClient.describe())
                .then((tc, testDto, client) -> {
                    String imageId = testDto.getResponse().getImage().getId();
                    if (!originalImageId.equals(imageId)) {
                        throw new TestFailException(
                                String.format("Image ID should be '%s' after switching back to URL catalog, but is: %s",
                                        originalImageId, imageId));
                    }
                    String catalog = testDto.getResponse().getImage().getCatalog();
                    if (!originalCatalogUrl.equals(catalog)) {
                        throw new TestFailException(
                                String.format("Catalog should be '%s' after switching back, but is: %s", originalCatalogUrl, catalog));
                    }
                    return testDto;
                })
                .validate();
    }

    private void createNamedFreeipaImageCatalog(MockedTestContext testContext, String catalogUrl) {
        testContext.given(ImageCatalogTestDto.class)
                .withName(NAMED_CATALOG)
                .withUrl(catalogUrl)
                .when(imageCatalogTestClient.createV4());
    }

}
