package com.sequenceiq.cloudbreak.service.upgrade.image.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.service.image.ImageTestBuilder;
import com.sequenceiq.cloudbreak.service.upgrade.image.ImageFilterParams;
import com.sequenceiq.cloudbreak.service.upgrade.image.ImageFilterResult;
import com.sequenceiq.cloudbreak.service.upgrade.validation.PythonVersionBasedRuntimeVersionValidator;

@ExtendWith(MockitoExtension.class)
class RuntimeDependencyBasedUpgradeImageFilterTest {

    @InjectMocks
    private RuntimeDependencyBasedUpgradeImageFilter underTest;

    @Mock
    private PythonVersionBasedRuntimeVersionValidator pythonVersionBasedRuntimeVersionValidator;

    @Test
    void testFilterShouldReturnTheImagesWithTheCorrectPythonVersion() {
        Image image1 = ImageTestBuilder.builder().withUuid("image1").build();
        Image image2 = ImageTestBuilder.builder().withUuid("image2").build();
        ImageFilterParams imageFilterParams = createImageFilterParams();

        when(pythonVersionBasedRuntimeVersionValidator.isUpgradePermittedForRuntime(imageFilterParams.getCurrentImage(), image1)).thenReturn(true);
        when(pythonVersionBasedRuntimeVersionValidator.isUpgradePermittedForRuntime(imageFilterParams.getCurrentImage(), image2)).thenReturn(false);

        ImageFilterResult actual = underTest.filter(new ImageFilterResult(List.of(image1, image2)), imageFilterParams);

        assertTrue(actual.getImages().contains(image1));
        assertFalse(actual.getImages().contains(image2));
        assertTrue(actual.getReason().isEmpty());
    }

    @Test
    void testFilterShouldReturnErrorMessageWhenThereAreNoCorrectImageLeft() {
        Image image1 = ImageTestBuilder.builder().withUuid("image1").build();
        Image image2 = ImageTestBuilder.builder().withUuid("image2").build();
        ImageFilterParams imageFilterParams = createImageFilterParams();

        when(pythonVersionBasedRuntimeVersionValidator.isUpgradePermittedForRuntime(imageFilterParams.getCurrentImage(), image1)).thenReturn(false);
        when(pythonVersionBasedRuntimeVersionValidator.isUpgradePermittedForRuntime(imageFilterParams.getCurrentImage(), image2)).thenReturn(false);

        ImageFilterResult actual = underTest.filter(new ImageFilterResult(List.of(image1, image2)), imageFilterParams);

        assertTrue(actual.getImages().isEmpty());
        assertEquals("There are no eligible images to upgrade because Python 3.8 dependency is missing from the current image", actual.getReason());
    }

    private ImageFilterParams createImageFilterParams() {
        return new ImageFilterParams(ImageTestBuilder.builder().withUuid("current").build(), false, null, null, null, null, null, null, null, null, false);
    }
}