package com.sequenceiq.cloudbreak.service.upgrade.image.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.service.image.ImageCatalogService;
import com.sequenceiq.cloudbreak.service.image.ImageTestBuilder;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.service.upgrade.image.ImageFilterParams;
import com.sequenceiq.cloudbreak.service.upgrade.image.ImageFilterResult;
import com.sequenceiq.cloudbreak.service.upgrade.validation.PythonVersionBasedRuntimeVersionValidator;
import com.sequenceiq.cloudbreak.service.upgrade.validation.service.PythonVersionValidator;
import com.sequenceiq.common.model.ImageCatalogPlatform;

@ExtendWith(MockitoExtension.class)
class RuntimeDependencyBasedUpgradeImageFilterTest {

    private static final long STACK_ID = 1L;

    private static final List<Image> IMAGES = Collections.emptyList();

    private static final long WORKSPACE_ID = 2L;

    private static final String IMAGE_CATALOG_NAME = "image-catalog-name";

    private static final String ACCOUNT_ID = "1234";

    private static final String STACK_CRN = "crn:cdp:sdx:us-west-1:1234:stack:mystack";

    @InjectMocks
    private RuntimeDependencyBasedUpgradeImageFilter underTest;

    @Mock
    private PythonVersionBasedRuntimeVersionValidator pythonVersionBasedRuntimeVersionValidator;

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private ImageCatalogService imageCatalogService;

    @Mock
    private StackDto stack;

    @Mock
    private com.sequenceiq.cloudbreak.cloud.model.Image currentImage;

    @Test
    void testFilterShouldReturnTheImagesWithTheCorrectPythonVersion() throws CloudbreakImageCatalogException {
        Image image1 = ImageTestBuilder.builder().withUuid("image1").build();
        Image image2 = ImageTestBuilder.builder().withUuid("image2").build();
        ImageFilterParams imageFilterParams = createImageFilterParams();

        when(stackDtoService.getById(STACK_ID)).thenReturn(stack);
        when(stack.getWorkspaceId()).thenReturn(WORKSPACE_ID);
        when(stack.getResourceCrn()).thenReturn(STACK_CRN);
        when(imageCatalogService.getAllCdhImages(eq(ACCOUNT_ID), eq(WORKSPACE_ID), eq(IMAGE_CATALOG_NAME), any())).thenReturn(IMAGES);
        when(pythonVersionBasedRuntimeVersionValidator.isUpgradePermittedForRuntime(stack, IMAGES, imageFilterParams.getCurrentImage(), image1))
                .thenReturn(true);
        when(pythonVersionBasedRuntimeVersionValidator.isUpgradePermittedForRuntime(stack, IMAGES, imageFilterParams.getCurrentImage(), image2))
                .thenReturn(false);

        ImageFilterResult actual = underTest.filter(new ImageFilterResult(List.of(image1, image2)), imageFilterParams);

        assertTrue(actual.getImages().contains(image1));
        assertFalse(actual.getImages().contains(image2));
        assertTrue(actual.getReason().isEmpty());
    }

    @Test
    void testFilterShouldReturnErrorMessageWhenThereAreNoCorrectImageLeft() throws CloudbreakImageCatalogException {
        Image image1 = ImageTestBuilder.builder().withUuid("image1").build();
        Image image2 = ImageTestBuilder.builder().withUuid("image2").build();
        ImageFilterParams imageFilterParams = createImageFilterParams();

        when(stackDtoService.getById(STACK_ID)).thenReturn(stack);
        when(stack.getWorkspaceId()).thenReturn(WORKSPACE_ID);
        when(stack.getResourceCrn()).thenReturn(STACK_CRN);
        when(imageCatalogService.getAllCdhImages(eq(ACCOUNT_ID), eq(WORKSPACE_ID), eq(IMAGE_CATALOG_NAME), any())).thenReturn(IMAGES);
        when(pythonVersionBasedRuntimeVersionValidator.isUpgradePermittedForRuntime(stack, IMAGES, imageFilterParams.getCurrentImage(), image1))
                .thenReturn(false);
        when(pythonVersionBasedRuntimeVersionValidator.isUpgradePermittedForRuntime(stack, IMAGES, imageFilterParams.getCurrentImage(), image2))
                .thenReturn(false);

        ImageFilterResult actual = underTest.filter(new ImageFilterResult(List.of(image1, image2)), imageFilterParams);

        assertTrue(actual.getImages().isEmpty());
        assertEquals(PythonVersionValidator.ERROR_MESSAGE, actual.getReason());
    }

    private ImageFilterParams createImageFilterParams() {
        return new ImageFilterParams(null, currentImage, IMAGE_CATALOG_NAME, false, null, null, null, STACK_ID, null,
                new ImageCatalogPlatform(CloudPlatform.AWS.name()), null, null, false);
    }
}