package com.sequenceiq.cloudbreak.service.upgrade.image.filter;

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

class ImageRegionBasedUpgradeImageFilterTest {

    private static final String IMAGE_ID_1 = "image1";

    private static final String IMAGE_ID_2 = "image2";

    private static final String REGION_1 = "us-west-1";

    private static final String REGION_2 = "us-west-2";

    private final ImageRegionBasedUpgradeImageFilter victim = new ImageRegionBasedUpgradeImageFilter();

    @Test
    public void testFilterReturnsAllImagesInCaseOfNullRegionInFilterParams() {
        List<Image> images = List.of(createImage(IMAGE_ID_1, REGION_1), createImage(IMAGE_ID_2, REGION_1));

        ImageFilterResult actual = victim.filter(createImageFilterResult(images), createImageFilterParams(null));

        assertEquals(images, actual.getImages());
        assertTrue(actual.getReason().isEmpty());
    }

    @Test
    public void testFilterReturnsImagesWithRegionFromFilterParams() {
        Image image1 = createImage(IMAGE_ID_1, REGION_1);
        Image image2 = createImage(IMAGE_ID_2, REGION_2);

        ImageFilterResult actual = victim.filter(createImageFilterResult(List.of(image1, image2)), createImageFilterParams(REGION_1));

        assertTrue(actual.getImages().contains(image1));
        assertFalse(actual.getImages().contains(image2));
        assertTrue(actual.getReason().isEmpty());
    }

    @Test
    public void testFilterReturnsEmptyImagesAsNoMatchingImageByRegionFromFilterParams() {
        Image image1 = createImage(IMAGE_ID_1, REGION_2);
        Image image2 = createImage(IMAGE_ID_2, REGION_2);

        ImageFilterResult actual = victim.filter(createImageFilterResult(List.of(image1, image2)), createImageFilterParams(REGION_1));

        assertTrue(actual.getImages().isEmpty());
        assertEquals("There are no eligible images to upgrade for us-west-1 region.", actual.getReason());
    }

    private ImageFilterParams createImageFilterParams(String region) {
        return new ImageFilterParams(null, false, null, null, null, null, null, null, region);
    }

    private Image createImage(String imageId, String region) {
        return new Image(null, null, null, null, null, imageId, null, null, Map.of("anyPlatform", Collections.singletonMap(region, "anyValue")), null, null,
                null, null, null, null, false, null, null);
    }

    private ImageFilterResult createImageFilterResult(List<Image> images) {
        return new ImageFilterResult(images, null);
    }
}