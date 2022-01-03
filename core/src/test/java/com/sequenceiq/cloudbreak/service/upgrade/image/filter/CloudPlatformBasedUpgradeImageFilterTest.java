package com.sequenceiq.cloudbreak.service.upgrade.image.filter;

import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.AWS;
import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.AZURE;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.service.upgrade.image.ImageFilterParams;
import com.sequenceiq.cloudbreak.service.upgrade.image.ImageFilterResult;

class CloudPlatformBasedUpgradeImageFilterTest {

    private static final String IMAGE_ID_1 = "image1";

    private static final String IMAGE_ID_2 = "image2";

    private final CloudPlatformBasedUpgradeImageFilter underTest = new CloudPlatformBasedUpgradeImageFilter();

    private final ImageFilterParams imageFilterParams = createImageFilterParams();

    @Test
    public void testFilterShouldReturnAllImage() {
        List<Image> images = List.of(createImage(IMAGE_ID_1, AWS.name()), createImage(IMAGE_ID_2, AWS.name()));

        ImageFilterResult actual = underTest.filter(createImageFilterResult(images), imageFilterParams);

        assertEquals(images, actual.getImages());
        assertTrue(actual.getReason().isEmpty());
    }

    @Test
    public void testFilterShouldReturnOnlyOneImage() {
        Image image1 = createImage(IMAGE_ID_1, AWS.name());
        Image image2 = createImage(IMAGE_ID_2, AZURE.name());
        List<Image> images = List.of(image1, image2);

        ImageFilterResult actual = underTest.filter(createImageFilterResult(images), imageFilterParams);

        assertTrue(actual.getImages().contains(image1));
        assertFalse(actual.getImages().contains(image2));
        assertTrue(actual.getReason().isEmpty());
    }

    @Test
    public void testFilterShouldReturnErrorMessageWithoutImages() {
        List<Image> images = List.of(createImage(IMAGE_ID_1, AZURE.name()), createImage(IMAGE_ID_2, AZURE.name()));

        ImageFilterResult actual = underTest.filter(createImageFilterResult(images), imageFilterParams);

        assertTrue(actual.getImages().isEmpty());
        assertEquals("There are no eligible images to upgrade for AWS cloud platform.", actual.getReason());
    }

    private ImageFilterParams createImageFilterParams() {
        return new ImageFilterParams(null, false, null, null, null, null, null, AWS.name());
    }

    private Image createImage(String imageId, String cloudPlatform) {
        return new Image(null, null, null, null, null, imageId, null, null, Map.of(cloudPlatform, Collections.emptyMap()), null, null, null, null, null, null,
                false, null, null);
    }

    private ImageFilterResult createImageFilterResult(List<Image> images) {
        return new ImageFilterResult(images, null);
    }

}