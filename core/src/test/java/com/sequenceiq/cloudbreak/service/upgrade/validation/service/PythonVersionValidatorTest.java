package com.sequenceiq.cloudbreak.service.upgrade.validation.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.common.exception.UpgradeValidationFailedException;
import com.sequenceiq.cloudbreak.common.service.PlatformStringTransformer;
import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.service.image.ImageCatalogService;
import com.sequenceiq.cloudbreak.service.image.ImageTestBuilder;
import com.sequenceiq.cloudbreak.service.image.ModelImageTestBuilder;
import com.sequenceiq.cloudbreak.service.image.StatedImage;
import com.sequenceiq.cloudbreak.service.upgrade.UpgradeImageInfo;
import com.sequenceiq.cloudbreak.service.upgrade.validation.PythonVersionBasedRuntimeVersionValidator;
import com.sequenceiq.common.model.ImageCatalogPlatform;

@ExtendWith(MockitoExtension.class)
class PythonVersionValidatorTest {

    private static final List<Image> CDH_IMAGES = Collections.emptyList();

    private static final String CLOUD_PLATFORM = "cloud-platform";

    private static final String PLATFORM_VARIANT = "platform-variant";

    private static final long WORKSPACE_ID = 2L;

    private static final String IMAGE_CATALOG_NAME = "image-catalog-name";

    private static final String CURRENT_IMAGE = "currentImage";

    private static final String ACCOUNT_ID = "1234";

    private static final String STACK_CRN = "crn:cdp:sdx:us-west-1:1234:stack:mystack";

    @InjectMocks
    private PythonVersionValidator underTest;

    @Mock
    private PythonVersionBasedRuntimeVersionValidator pythonVersionBasedRuntimeVersionValidator;

    @Mock
    private ImageCatalogService imageCatalogService;

    @Mock
    private PlatformStringTransformer platformStringTransformer;

    @Mock
    private StackDto stack;

    @Mock
    private Set<ImageCatalogPlatform> imageCatalogPlatformSet;

    @BeforeEach
    void before() throws CloudbreakImageCatalogException {
        when(stack.getWorkspaceId()).thenReturn(WORKSPACE_ID);
        when(stack.getCloudPlatform()).thenReturn(CLOUD_PLATFORM);
        when(stack.getPlatformVariant()).thenReturn(PLATFORM_VARIANT);
        when(stack.getResourceCrn()).thenReturn(STACK_CRN);
        when(platformStringTransformer.getPlatformStringForImageCatalogSet(CLOUD_PLATFORM, PLATFORM_VARIANT)).thenReturn(imageCatalogPlatformSet);
        when(imageCatalogService.getAllCdhImages(eq(ACCOUNT_ID), eq(WORKSPACE_ID), eq(IMAGE_CATALOG_NAME), eq(imageCatalogPlatformSet))).thenReturn(CDH_IMAGES);
    }

    @Test
    void testValidateShouldThrowValidationExceptionWhenTheUpgradeIsNotPermittedForTheTargetImage() {
        com.sequenceiq.cloudbreak.cloud.model.Image currentImage = createCurrentImage();
        Image targetImage = ImageTestBuilder.builder().withUuid("targetImage").build();

        when(pythonVersionBasedRuntimeVersionValidator.isUpgradePermittedForRuntime(stack, CDH_IMAGES, currentImage, targetImage)).thenReturn(false);

        assertThrows(UpgradeValidationFailedException.class, () -> underTest.validate(createValidationRequest(currentImage, targetImage)));
    }

    @Test
    void testValidateShouldNotThrowValidationExceptionWhenTheUpgradeIsPermittedForTheTargetImage() {
        com.sequenceiq.cloudbreak.cloud.model.Image currentImage = createCurrentImage();
        Image targetImage = ImageTestBuilder.builder().withUuid("targetImage").build();

        when(pythonVersionBasedRuntimeVersionValidator.isUpgradePermittedForRuntime(stack, CDH_IMAGES, currentImage, targetImage)).thenReturn(true);

        underTest.validate(createValidationRequest(currentImage, targetImage));
    }

    private ServiceUpgradeValidationRequest createValidationRequest(com.sequenceiq.cloudbreak.cloud.model.Image currentImage, Image targetImage) {
        return new ServiceUpgradeValidationRequest(stack, false, true,
                new UpgradeImageInfo(currentImage, StatedImage.statedImage(targetImage, null, null)), false);
    }

    private com.sequenceiq.cloudbreak.cloud.model.Image createCurrentImage() {
        return ModelImageTestBuilder.builder().withImageId(CURRENT_IMAGE).withImageCatalogName(IMAGE_CATALOG_NAME).build();
    }
}