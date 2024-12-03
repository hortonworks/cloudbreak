package com.sequenceiq.cloudbreak.service.upgrade.image.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.service.upgrade.image.CentosToRedHatUpgradeCondition;
import com.sequenceiq.cloudbreak.service.upgrade.image.ImageFilterParams;
import com.sequenceiq.cloudbreak.service.upgrade.image.ImageFilterResult;

@ExtendWith(MockitoExtension.class)
class OsVersionBasedUpgradeImageFilterTest {

    private static final String CURRENT_OS = "centos";

    private static final String CURRENT_OS_TYPE = "redhat7";

    private static final long STACK_ID = 1L;

    @InjectMocks
    private OsVersionBasedUpgradeImageFilter underTest;

    @Mock
    private CentosToRedHatUpgradeCondition centosToRedHatUpgradeCondition;

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
        return new ImageFilterParams(null, createCurrentImage(), null, false, null, null, null, STACK_ID, null, null, null, null, false);
    }

    private Image createImage(String imageId, String os, String osType) {
        return Image.builder().withUuid(imageId).withOs(os).withOsType(osType).build();
    }

    private com.sequenceiq.cloudbreak.cloud.model.Image createCurrentImage() {
        return com.sequenceiq.cloudbreak.cloud.model.Image.builder().withImageId("current-image").withOs(CURRENT_OS).withOsType(CURRENT_OS_TYPE).build();
    }

    private ImageFilterResult createImageFilterResult(List<Image> images) {
        return new ImageFilterResult(images, null);
    }

}