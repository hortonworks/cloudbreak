package com.sequenceiq.cloudbreak.service.upgrade.image.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.ImagePackageVersion;
import com.sequenceiq.cloudbreak.service.upgrade.image.ImageFilterResult;

class NonCmUpgradeImageFilterTest {

    private final NonCmUpgradeImageFilter underTest = new NonCmUpgradeImageFilter();

    @Test
    public void testFilterShouldReturnAllImages() {
        Map<String, String> packageVersions = Map.of(ImagePackageVersion.CM.getKey(), "7.2.3");
        List<Image> images = List.of(createImage("image1", packageVersions), createImage("image2", packageVersions));

        ImageFilterResult actual = underTest.filter(createImageFilterResult(images), null);

        assertEquals(images, actual.getImages());
        assertTrue(actual.getReason().isEmpty());
    }

    @Test
    public void testFilterShouldRemoveImagesWithIgnoredCmVersion() {
        Image image1 = createImage("image1", Map.of(ImagePackageVersion.CM.getKey(), "7.2.1"));
        Image image2 = createImage("image2", Map.of(ImagePackageVersion.CM.getKey(), "7.2.2"));
        Image image3 = createImage("image3", Collections.emptyMap());

        ImageFilterResult actual = underTest.filter(createImageFilterResult(List.of(image1, image2, image3)), null);

        assertTrue(actual.getImages().contains(image1));
        assertTrue(actual.getImages().contains(image2));
        assertFalse(actual.getImages().contains(image3));
        assertTrue(actual.getReason().isEmpty());
    }

    @Test
    public void testFilterShouldReturnErrorMessageWhenAllImagesAreRemoved() {
        List<Image> images = List.of(createImage("image1", Collections.emptyMap()), createImage("image2", Collections.emptyMap()));

        ImageFilterResult actual = underTest.filter(createImageFilterResult(images), null);

        assertTrue(actual.getImages().isEmpty());
        assertEquals("There are no eligible images to upgrade available with Cloudera Manager packages.", actual.getReason());
    }

    private Image createImage(String imageId, Map<String, String> packageVersions) {
        return new Image(null, null, null, null, null, imageId, null, null, null, null, null, packageVersions, null, null, null,
                false, null, null);
    }

    private ImageFilterResult createImageFilterResult(List<Image> images) {
        return new ImageFilterResult(images, null);
    }
}