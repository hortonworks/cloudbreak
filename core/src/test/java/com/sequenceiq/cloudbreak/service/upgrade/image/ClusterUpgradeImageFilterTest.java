package com.sequenceiq.cloudbreak.service.upgrade.image;

import static com.sequenceiq.cloudbreak.service.image.catalog.model.ImageCatalogPlatform.imageCatalogPlatform;
import static com.sequenceiq.cloudbreak.service.upgrade.image.ImageFilterResult.EMPTY_REASON;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.InternalUpgradeSettings;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.service.image.ImageCatalogService;
import com.sequenceiq.cloudbreak.service.upgrade.image.filter.ImageFilterUpgradeService;

@ExtendWith(MockitoExtension.class)
public class ClusterUpgradeImageFilterTest {

    private static final String CLOUD_PLATFORM = "aws";

    private static final long STACK_ID = 1L;

    private static final String ACCOUNT_ID = "account1";

    private static final String BLUEPRINT_ERROR = "Invalid blueprint";

    private static final String IMAGE_CATALOG_NAME = "image catalog";

    private static final Long WORKSPACE_ID = 2L;

    private static final String REGION = "us-west-1";

    @InjectMocks
    private ClusterUpgradeImageFilter underTest;

    @Mock
    private BlueprintBasedUpgradeValidator blueprintBasedUpgradeValidator;

    @Mock
    private ImageCatalogService imageCatalogService;

    @Mock
    private ImageFilterUpgradeService imageFilterUpgradeService;

    @Mock
    private Image image;

    @Mock
    private Image otherImage;

    private final ImageFilterParams imageFilterParams = createImageFilterParams();

    @Test
    void testFilterWithImageCatalogShouldReturnErrorMessageWhenTheBlueprintIsNotEligible() {
        when(blueprintBasedUpgradeValidator.isValidBlueprint(imageFilterParams, ACCOUNT_ID)).thenReturn(new BlueprintValidationResult(false, BLUEPRINT_ERROR));

        ImageFilterResult actual = underTest.filter(ACCOUNT_ID, WORKSPACE_ID, IMAGE_CATALOG_NAME, imageFilterParams);

        assertTrue(actual.getImages().isEmpty());
        assertEquals(BLUEPRINT_ERROR, actual.getReason());
        verify(blueprintBasedUpgradeValidator).isValidBlueprint(imageFilterParams, ACCOUNT_ID);
        verifyNoInteractions(imageCatalogService);
        verifyNoInteractions(imageFilterUpgradeService);
    }

    @Test
    void testFilterShouldReturnErrorMessageWhenTheBlueprintIsNotEligible() {
        when(blueprintBasedUpgradeValidator.isValidBlueprint(imageFilterParams, ACCOUNT_ID)).thenReturn(new BlueprintValidationResult(false, BLUEPRINT_ERROR));

        ImageFilterResult actual = underTest.filter(ACCOUNT_ID, WORKSPACE_ID, IMAGE_CATALOG_NAME, imageFilterParams);

        assertTrue(actual.getImages().isEmpty());
        assertEquals(BLUEPRINT_ERROR, actual.getReason());
        verify(blueprintBasedUpgradeValidator).isValidBlueprint(imageFilterParams, ACCOUNT_ID);
        verifyNoInteractions(imageCatalogService);
        verifyNoInteractions(imageFilterUpgradeService);
    }

    @Test
    void testFilterWithImageCatalogShouldReturnErrorMessageWhenThereAreNoAvailableCandidateImage() throws CloudbreakImageCatalogException {
        String errorMessage = "There are no available image";
        ImageFilterResult imageFilterResult = new ImageFilterResult(Collections.emptyList(), errorMessage);
        when(blueprintBasedUpgradeValidator.isValidBlueprint(imageFilterParams, ACCOUNT_ID)).thenReturn(new BlueprintValidationResult(true));
        when(imageCatalogService
                .getImageFilterResult(WORKSPACE_ID, IMAGE_CATALOG_NAME, imageFilterParams.getImageCatalogPlatform(), imageFilterParams.isGetAllImages()))
                .thenReturn(imageFilterResult);

        ImageFilterResult actual = underTest.filter(ACCOUNT_ID, WORKSPACE_ID, IMAGE_CATALOG_NAME, imageFilterParams);

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
        when(blueprintBasedUpgradeValidator.isValidBlueprint(imageFilterParams, ACCOUNT_ID)).thenReturn(new BlueprintValidationResult(true));
        when(imageCatalogService
                .getImageFilterResult(WORKSPACE_ID, IMAGE_CATALOG_NAME, imageFilterParams.getImageCatalogPlatform(), imageFilterParams.isGetAllImages()))
                .thenReturn(imageFilterResult);
        when(imageFilterUpgradeService.filterImages(imageFilterResult, imageFilterParams)).thenReturn(otherImageFilterResult);

        ImageFilterResult actual = underTest.filter(ACCOUNT_ID, WORKSPACE_ID, IMAGE_CATALOG_NAME, imageFilterParams);

        assertEquals(actual, otherImageFilterResult);
    }

    private ImageFilterParams createImageFilterParams() {
        return new ImageFilterParams(image, false, Collections.emptyMap(), StackType.DATALAKE, new Blueprint(), STACK_ID,
                new InternalUpgradeSettings(false, true, true), imageCatalogPlatform(CLOUD_PLATFORM),
                CLOUD_PLATFORM, REGION, false);
    }

}