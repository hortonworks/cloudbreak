package com.sequenceiq.cloudbreak.service.upgrade.image.filter;

import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.AWS;
import static com.sequenceiq.common.model.ImageCatalogPlatform.imageCatalogPlatform;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.service.image.ImageService;
import com.sequenceiq.cloudbreak.service.upgrade.image.ImageFilterParams;
import com.sequenceiq.cloudbreak.service.upgrade.image.ImageFilterResult;

@ExtendWith(MockitoExtension.class)
class ImageRegionUpgradeImageFilterTest {

    private static final String IMAGE_ID_1 = "image1";

    private static final String IMAGE_ID_2 = "image2";

    private static final String PLATFORM = AWS.name();

    private static final String REGION = "us-west-1";

    @Mock
    private ImageService imageService;

    @InjectMocks
    private ImageRegionUpgradeImageFilter victim;

    @Test
    public void testFilterImagesWithoutName() throws CloudbreakImageNotFoundException {
        Image image1 = createImage(IMAGE_ID_1);
        Image image2 = createImage(IMAGE_ID_2);
        ImageFilterParams imageFilterParams = createImageFilterParams(PLATFORM, REGION);

        when(imageService.determineImageName(
                imageFilterParams.getCloudPlatform(), imageFilterParams.getImageCatalogPlatform(), imageFilterParams.getRegion(), image1)
        ).thenReturn(IMAGE_ID_1);
        when(imageService.determineImageName(
                imageFilterParams.getCloudPlatform(), imageFilterParams.getImageCatalogPlatform(), imageFilterParams.getRegion(), image2)
        ).thenThrow(new CloudbreakImageNotFoundException(""));

        ImageFilterResult actual = victim.filter(createImageFilterResult(image1, image2), imageFilterParams);

        assertTrue(actual.getImages().contains(image1));
        assertFalse(actual.getImages().contains(image2));
        assertTrue(actual.getReason().isEmpty());
    }

    @Test
    public void testErrorMessageInCaseOfEmptyResult() throws CloudbreakImageNotFoundException {
        Image image1 = createImage(IMAGE_ID_1);
        Image image2 = createImage(IMAGE_ID_2);
        ImageFilterParams imageFilterParams = createImageFilterParams(PLATFORM, REGION);

        when(imageService.determineImageName(
                imageFilterParams.getCloudPlatform(), imageFilterParams.getImageCatalogPlatform(), imageFilterParams.getRegion(), image1)
        ).thenThrow(new CloudbreakImageNotFoundException(""));
        when(imageService.determineImageName(
                imageFilterParams.getCloudPlatform(), imageFilterParams.getImageCatalogPlatform(), imageFilterParams.getRegion(), image2)
        ).thenThrow(new CloudbreakImageNotFoundException(""));

        ImageFilterResult actual = victim.filter(createImageFilterResult(image1, image2), imageFilterParams);

        assertTrue(actual.getImages().isEmpty());
        assertEquals("There are no eligible images to upgrade for 'AWS' image catalog platform, 'AWS' cloud platform and 'us-west-1' region.",
                actual.getReason());
    }

    private Image createImage(String imageId) {
        return Image.builder().withUuid(imageId).build();
    }

    private ImageFilterResult createImageFilterResult(Image... images) {
        return new ImageFilterResult(Arrays.asList(images), null);
    }

    private ImageFilterParams createImageFilterParams(String platform, String region) {
        return new ImageFilterParams(null, null, null, false, false, null, null, null, null, null, imageCatalogPlatform(platform), platform, region, false);
    }
}