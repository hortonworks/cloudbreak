package com.sequenceiq.cloudbreak.service.upgrade.image.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.service.upgrade.image.ImageFilterParams;
import com.sequenceiq.cloudbreak.service.upgrade.image.ImageFilterResult;

class OsVersionBasedUpgradeImageFilterTest {

    private static final String CURRENT_OS = "centos";

    private static final String CURRENT_OS_TYPE = "redhat7";

    private final OsVersionBasedUpgradeImageFilter underTest = new OsVersionBasedUpgradeImageFilter();

    @Test
    public void testFilterShouldReturnAllImages() {
        List<Image> images = List.of(createImage("image1", CURRENT_OS, CURRENT_OS_TYPE), createImage("image2", CURRENT_OS, CURRENT_OS_TYPE));

        ImageFilterResult actual = underTest.filter(createImageFilterResult(images), createImageFilterParams());

        assertEquals(images, actual.getImages());
        assertTrue(actual.getReason().isEmpty());
    }

    @Test
    public void testFilterShouldReturnImagesWithTheSameOsAndOsType() {
        Image image1 = createImage("image1", CURRENT_OS, CURRENT_OS_TYPE);
        Image image2 = createImage("image2", "ubuntu", "amazon-linux");

        ImageFilterResult actual = underTest.filter(createImageFilterResult(List.of(image1, image2)), createImageFilterParams());

        assertTrue(actual.getImages().contains(image1));
        assertFalse(actual.getImages().contains(image2));
        assertTrue(actual.getReason().isEmpty());
    }

    @Test
    public void testFilterShouldReturnImagesWithTheSameOsAndOsTypeWheOnlyTheOsIsDifferent() {
        Image image1 = createImage("image1", CURRENT_OS, CURRENT_OS_TYPE);
        Image image2 = createImage("image2", "ubuntu", CURRENT_OS_TYPE);

        ImageFilterResult actual = underTest.filter(createImageFilterResult(List.of(image1, image2)), createImageFilterParams());

        assertTrue(actual.getImages().contains(image1));
        assertFalse(actual.getImages().contains(image2));
        assertTrue(actual.getReason().isEmpty());
    }

    @Test
    public void testFilterShouldReturnImagesWithTheSameOsAndOsTypeWheOnlyTheOsTypeIsDifferent() {
        Image image1 = createImage("image1", CURRENT_OS, CURRENT_OS_TYPE);
        Image image2 = createImage("image2", CURRENT_OS, "amazon-linux");

        ImageFilterResult actual = underTest.filter(createImageFilterResult(List.of(image1, image2)), createImageFilterParams());

        assertTrue(actual.getImages().contains(image1));
        assertFalse(actual.getImages().contains(image2));
        assertTrue(actual.getReason().isEmpty());
    }

    @Test
    public void testFilterShouldReturnErrorMessageWhenThereAreNoImageWithSameOsAndOsType() {
        List<Image> images = List.of(createImage("image1", CURRENT_OS, "ubuntu"), createImage("image2", CURRENT_OS, "amazon-linux"));

        ImageFilterResult actual = underTest.filter(createImageFilterResult(images), createImageFilterParams());

        assertTrue(actual.getImages().isEmpty());
        assertEquals("There are no eligible images to upgrade with the same OS version.", actual.getReason());
    }

    private ImageFilterParams createImageFilterParams() {
        return new ImageFilterParams(createImage("current-image", CURRENT_OS, CURRENT_OS_TYPE), false, null, null, null, null, null, null);
    }

    private Image createImage(String imageId, String os, String osType) {
        return new Image(null, null, null, null, os, imageId, null, null, null, null, osType, null, null, null, null, false, null, null);
    }

    private ImageFilterResult createImageFilterResult(List<Image> images) {
        return new ImageFilterResult(images, null);
    }

}