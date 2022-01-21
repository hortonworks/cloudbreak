package com.sequenceiq.cloudbreak.service.upgrade.image.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.service.image.CurrentImageUsageCondition;
import com.sequenceiq.cloudbreak.service.upgrade.image.ImageFilterParams;
import com.sequenceiq.cloudbreak.service.upgrade.image.ImageFilterResult;

@ExtendWith(MockitoExtension.class)
class CurrentImageUpgradeImageFilterTest {

    private static final String CURRENT_IMAGE_ID = "current-image";

    private static final long CURRENT_STACK_ID = 1L;

    @InjectMocks
    private CurrentImageUpgradeImageFilter underTest;

    @Mock
    private CurrentImageUsageCondition currentImageUsageCondition;

    private final ImageFilterParams imageFilterParams = createImageFilterParams();

    @Test
    public void testFilterShouldReturnAllImage() {
        List<Image> images = List.of(createImage("image1"), createImage(CURRENT_IMAGE_ID));
        when(currentImageUsageCondition.currentImageUsedOnInstances(CURRENT_STACK_ID, CURRENT_IMAGE_ID)).thenReturn(true);

        ImageFilterResult actual = underTest.filter(createImageFilterResult(images), imageFilterParams);

        assertEquals(images, actual.getImages());
        assertTrue(actual.getReason().isEmpty());
        verify(currentImageUsageCondition).currentImageUsedOnInstances(CURRENT_STACK_ID, CURRENT_IMAGE_ID);
    }

    @Test
    public void testFilterShouldReturnImagesWithoutCurrentImageWhenTheCurrentImageFilteringIsNotAllowed() {
        Image image1 = createImage("image1");
        Image currentImage = createImage(CURRENT_IMAGE_ID);
        when(currentImageUsageCondition.currentImageUsedOnInstances(CURRENT_STACK_ID, CURRENT_IMAGE_ID)).thenReturn(false);

        ImageFilterResult actual = underTest.filter(createImageFilterResult(List.of(image1, currentImage)), imageFilterParams);

        assertTrue(actual.getImages().contains(image1));
        assertFalse(actual.getImages().contains(currentImage));
        assertTrue(actual.getReason().isEmpty());
        verify(currentImageUsageCondition).currentImageUsedOnInstances(CURRENT_STACK_ID, CURRENT_IMAGE_ID);
    }

    @Test
    public void testFilterShouldReturnImagesWithoutCurrentImageTheInputListDoesNotContainsTheCurrentImage() {
        List<Image> images = List.of(createImage("image1"), createImage("image2"));

        ImageFilterResult actual = underTest.filter(createImageFilterResult(images), imageFilterParams);

        assertEquals(images, actual.getImages());
        assertTrue(actual.getReason().isEmpty());
        verifyNoInteractions(currentImageUsageCondition);
    }

    private ImageFilterParams createImageFilterParams() {
        return new ImageFilterParams(createImage(CURRENT_IMAGE_ID), false, null, null, null, CURRENT_STACK_ID, null, null);
    }

    private Image createImage(String imageId) {
        return new Image(null, null, null, null, null, imageId, null, null, null, null, null, null, null, null, null, false, null, null);
    }

    private ImageFilterResult createImageFilterResult(List<Image> images) {
        return new ImageFilterResult(images, null);
    }
}