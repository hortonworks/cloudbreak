package com.sequenceiq.cloudbreak.service.upgrade.image.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.ImagePackageVersion;
import com.sequenceiq.cloudbreak.service.upgrade.image.ImageFilterResult;

class IgnoredCmVersionUpgradeImageFilterTest {

    private static final String IGNORED_CM_VERSION = "7.x.0";

    private final IgnoredCmVersionUpgradeImageFilter underTest = new IgnoredCmVersionUpgradeImageFilter();

    @Test
    public void testFilterShouldReturnAllImages() {
        List<Image> images = List.of(createImage("image1", "7.2.1"), createImage("image2", "7.2.2"));

        ImageFilterResult actual = underTest.filter(createImageFilterResult(images), null);

        assertEquals(images, actual.getImages());
        assertTrue(actual.getReason().isEmpty());
    }

    @Test
    public void testFilterShouldRemoveImagesWithIgnoredCmVersion() {
        Image image1 = createImage("image1", "7.2.1");
        Image image2 = createImage("image2", "7.2.2");
        Image image3 = createImage("image3", "7.x.0");

        ImageFilterResult actual = underTest.filter(createImageFilterResult(List.of(image1, image2, image3)), null);

        assertTrue(actual.getImages().contains(image1));
        assertTrue(actual.getImages().contains(image2));
        assertFalse(actual.getImages().contains(image3));
        assertTrue(actual.getReason().isEmpty());
    }

    @Test
    public void testFilterShouldReturnErrorMessageWhenAllImagesAreRemoved() {
        List<Image> images = List.of(createImage("image1", IGNORED_CM_VERSION), createImage("image2", IGNORED_CM_VERSION));

        ImageFilterResult actual = underTest.filter(createImageFilterResult(images), null);

        assertTrue(actual.getImages().isEmpty());
        assertEquals("There are no eligible images with supported Cloudera Manager or CDP version.", actual.getReason());
    }

    private Image createImage(String imageId, String cmVersion) {
        return new Image(null, null, null, null, null, imageId, null, null, null, null, null, Map.of(ImagePackageVersion.CM.getKey(), cmVersion), null, null,
                null, false, null, null);
    }

    private ImageFilterResult createImageFilterResult(List<Image> images) {
        return new ImageFilterResult(images, null);
    }

}