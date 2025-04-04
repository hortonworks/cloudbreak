package com.sequenceiq.cloudbreak.service.upgrade.image;

import static com.sequenceiq.cloudbreak.service.upgrade.image.ImageFilterResult.EMPTY_REASON;
import static com.sequenceiq.common.model.ImageCatalogPlatform.imageCatalogPlatform;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.InternalUpgradeSettings;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.ImagePackageVersion;
import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.service.image.ImageCatalogService;
import com.sequenceiq.cloudbreak.service.upgrade.image.filter.ImageFilterUpgradeService;

@ExtendWith(MockitoExtension.class)
public class ClusterUpgradeImageFilterTest {

    private static final String CLOUD_PLATFORM = "aws";

    private static final long STACK_ID = 1L;

    private static final String BLUEPRINT_ERROR = "Invalid blueprint";

    private static final String IMAGE_CATALOG_NAME = "image catalog";

    private static final String FALLBACK_CATALOG_URL = "/images/fallback";

    private static final String CURRENT_IMAGE_ID = "16ad7759-83b1-42aa-aadf-0e3a6e7b5444";

    private static final Long WORKSPACE_ID = 2L;

    private static final String REGION = "us-west-1";

    private static final String INTERNAL_ERROR = "Failed to retrieve eligible images due to an internal error.";

    private static final String BASE_IMAGE_ERROR = "Cannot upgrade a base image cluster.";

    private static final String BUILD_NUMBER = "64507825";

    private static final String CM_VERSION = "7.3.1";

    private final ImageFilterParams imageFilterParams = createImageFilterParams();

    @InjectMocks
    private ClusterUpgradeImageFilter underTest;

    @Mock
    private BlueprintUpgradeOptionValidator blueprintUpgradeOptionValidator;

    @Mock
    private ImageCatalogService imageCatalogService;

    @Mock
    private ImageFilterUpgradeService imageFilterUpgradeService;

    @Mock
    private Image image;

    @Mock
    private Image otherImage;

    @Test
    void testFilterWithImageCatalogShouldReturnErrorMessageWhenTheBlueprintIsNotEligible() {
        when(blueprintUpgradeOptionValidator.isValidBlueprint(imageFilterParams)).thenReturn(new BlueprintValidationResult(false, BLUEPRINT_ERROR));

        ImageFilterResult actual = underTest.getAvailableImagesForUpgrade(WORKSPACE_ID, IMAGE_CATALOG_NAME, imageFilterParams);

        assertTrue(actual.getImages().isEmpty());
        assertEquals(BLUEPRINT_ERROR, actual.getReason());
        verify(blueprintUpgradeOptionValidator).isValidBlueprint(imageFilterParams);
        verifyNoInteractions(imageCatalogService);
        verifyNoInteractions(imageFilterUpgradeService);
    }

    @Test
    void testFilterShouldReturnErrorMessageWhenTheBlueprintIsNotEligible() {
        when(blueprintUpgradeOptionValidator.isValidBlueprint(imageFilterParams)).thenReturn(new BlueprintValidationResult(false, BLUEPRINT_ERROR));

        ImageFilterResult actual = underTest.getAvailableImagesForUpgrade(WORKSPACE_ID, IMAGE_CATALOG_NAME, imageFilterParams);

        assertTrue(actual.getImages().isEmpty());
        assertEquals(BLUEPRINT_ERROR, actual.getReason());
        verify(blueprintUpgradeOptionValidator).isValidBlueprint(imageFilterParams);
        verifyNoInteractions(imageCatalogService);
        verifyNoInteractions(imageFilterUpgradeService);
    }

    @Test
    void testFilterWithImageCatalogShouldReturnErrorMessageWhenThereAreNoAvailableCandidateImage() throws CloudbreakImageCatalogException {
        String errorMessage = "There are no available image";
        ImageFilterResult imageFilterResult = new ImageFilterResult(Collections.emptyList(), errorMessage);
        when(blueprintUpgradeOptionValidator.isValidBlueprint(imageFilterParams)).thenReturn(new BlueprintValidationResult(true));
        when(imageCatalogService
                .getImageFilterResult(WORKSPACE_ID, IMAGE_CATALOG_NAME, imageFilterParams.getImageCatalogPlatform(), imageFilterParams.isGetAllImages(),
                        imageFilterParams.getCurrentImage().getImageId())).thenReturn(imageFilterResult);

        ImageFilterResult actual = underTest.getAvailableImagesForUpgrade(WORKSPACE_ID, IMAGE_CATALOG_NAME, imageFilterParams);

        assertTrue(actual.getImages().isEmpty());
        assertEquals(errorMessage, actual.getReason());
        verifyNoInteractions(imageFilterUpgradeService);
    }

    @Test
    void testFilterWithImageCatalogShouldReturnFilteredImages() throws CloudbreakImageCatalogException {
        List<Image> images = List.of(image);
        List<Image> otherImages = List.of(otherImage);
        ImageFilterResult imageFilterResult = new ImageFilterResult(images, EMPTY_REASON);
        ImageFilterResult otherImageFilterResult = new ImageFilterResult(otherImages, EMPTY_REASON);
        when(blueprintUpgradeOptionValidator.isValidBlueprint(imageFilterParams)).thenReturn(new BlueprintValidationResult(true));
        when(imageCatalogService.getImageFilterResult(WORKSPACE_ID, IMAGE_CATALOG_NAME, imageFilterParams.getImageCatalogPlatform(),
                imageFilterParams.isGetAllImages(), imageFilterParams.getCurrentImage().getImageId())).thenReturn(imageFilterResult);
        when(imageFilterUpgradeService.filterImages(imageFilterResult, imageFilterParams)).thenReturn(otherImageFilterResult);

        ImageFilterResult actual = underTest.getAvailableImagesForUpgrade(WORKSPACE_ID, IMAGE_CATALOG_NAME, imageFilterParams);

        assertEquals(actual, otherImageFilterResult);
    }

    @Test
    public void testGetAvailableImagesForUpgradeShouldReturnErrorOnException() throws CloudbreakImageCatalogException {
        ImageFilterParams imageFilterParams = createImageFilterParams();

        when(blueprintUpgradeOptionValidator.isValidBlueprint(imageFilterParams)).thenReturn(new BlueprintValidationResult(true));
        when(imageCatalogService.getImageFilterResult(WORKSPACE_ID, IMAGE_CATALOG_NAME, imageFilterParams.getImageCatalogPlatform(),
                imageFilterParams.isGetAllImages(), imageFilterParams.getCurrentImage().getImageId())).thenThrow(new RuntimeException("Internal Error"));

        ImageFilterResult availableImagesForUpgrade = underTest.getAvailableImagesForUpgrade(WORKSPACE_ID, IMAGE_CATALOG_NAME, imageFilterParams);

        assertTrue(availableImagesForUpgrade.getImages().isEmpty());
        assertEquals(INTERNAL_ERROR, availableImagesForUpgrade.getReason());

    }

    @Test
    void testFilterShouldReturnErrorMessageWhenCurrentImageIsBaseImage() {
        ImageFilterParams imageFilterParamsWithBaseImage = createImageFilterParams(createCurrentImageWithoutPackageVersions());
        ImageFilterResult actual = underTest.getAvailableImagesForUpgrade(WORKSPACE_ID, IMAGE_CATALOG_NAME, imageFilterParamsWithBaseImage);

        assertTrue(actual.getImages().isEmpty());
        assertEquals(BASE_IMAGE_ERROR, actual.getReason());
        verifyNoInteractions(blueprintUpgradeOptionValidator);
        verifyNoInteractions(imageCatalogService);
        verifyNoInteractions(imageFilterUpgradeService);
    }

    private ImageFilterParams createImageFilterParams() {
        return createImageFilterParams(createCurrentImage());
    }

    private ImageFilterParams createImageFilterParams(com.sequenceiq.cloudbreak.cloud.model.Image currentImage) {
        return new ImageFilterParams(null, currentImage,
                null, false, false, Collections.emptyMap(), StackType.DATALAKE, new Blueprint(), STACK_ID, new InternalUpgradeSettings(false, true, true),
                imageCatalogPlatform(CLOUD_PLATFORM), CLOUD_PLATFORM, REGION, false);
    }

    private com.sequenceiq.cloudbreak.cloud.model.Image createCurrentImageWithoutPackageVersions() {
        return com.sequenceiq.cloudbreak.cloud.model.Image.builder()
                .withImageId(CURRENT_IMAGE_ID)
                .withImageCatalogName(IMAGE_CATALOG_NAME)
                .withImageCatalogUrl(FALLBACK_CATALOG_URL)
                .build();
    }

    private com.sequenceiq.cloudbreak.cloud.model.Image createCurrentImage() {
        return com.sequenceiq.cloudbreak.cloud.model.Image.builder()
                .withImageId(CURRENT_IMAGE_ID)
                .withImageCatalogName(IMAGE_CATALOG_NAME)
                .withImageCatalogUrl(FALLBACK_CATALOG_URL)
                .withPackageVersions(getPackageVersions())
                .build();
    }

    private static Map<String, String> getPackageVersions() {
        Map<String, String> packageVersions = new HashMap<>();
        packageVersions.put(ImagePackageVersion.CM.getKey(), CM_VERSION);
        packageVersions.put(ImagePackageVersion.CDH_BUILD_NUMBER.getKey(), BUILD_NUMBER);
        return packageVersions;
    }

}