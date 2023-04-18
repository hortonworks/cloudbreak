package com.sequenceiq.cloudbreak.service.upgrade.validation.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.common.exception.UpgradeValidationFailedException;
import com.sequenceiq.cloudbreak.service.image.ImageTestBuilder;
import com.sequenceiq.cloudbreak.service.image.StatedImage;
import com.sequenceiq.cloudbreak.service.upgrade.UpgradeImageInfo;
import com.sequenceiq.cloudbreak.service.upgrade.validation.PythonVersionBasedRuntimeVersionValidator;

@ExtendWith(MockitoExtension.class)
class PythonVersionValidatorTest {

    @InjectMocks
    private PythonVersionValidator underTest;

    @Mock
    private PythonVersionBasedRuntimeVersionValidator pythonVersionBasedRuntimeVersionValidator;

    @Test
    void testValidateShouldThrowValidationExceptionWhenTheUpgradeIsNotPermittedForTheTargetImage() {
        Image currentImage = ImageTestBuilder.builder().withUuid("currentImage").build();
        Image targetImage = ImageTestBuilder.builder().withUuid("targetImage").build();

        when(pythonVersionBasedRuntimeVersionValidator.isUpgradePermittedForRuntime(currentImage, targetImage)).thenReturn(false);

        assertThrows(UpgradeValidationFailedException.class, () -> underTest.validate(createValidationRequest(currentImage, targetImage)));
    }

    @Test
    void testValidateShouldNotThrowValidationExceptionWhenTheUpgradeIsPermittedForTheTargetImage() {
        Image currentImage = ImageTestBuilder.builder().withUuid("currentImage").build();
        Image targetImage = ImageTestBuilder.builder().withUuid("targetImage").build();

        when(pythonVersionBasedRuntimeVersionValidator.isUpgradePermittedForRuntime(currentImage, targetImage)).thenReturn(true);

        underTest.validate(createValidationRequest(currentImage, targetImage));
    }

    private ServiceUpgradeValidationRequest createValidationRequest(Image currentImage, Image targetImage) {
        return new ServiceUpgradeValidationRequest(null, false, null,
                new UpgradeImageInfo(null, StatedImage.statedImage(currentImage, null, null), StatedImage.statedImage(targetImage, null, null)));
    }
}