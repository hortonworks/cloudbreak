package com.sequenceiq.cloudbreak.service.upgrade.sync;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType.WORKLOAD;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.ImageStackDetails;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.service.image.StatedImage;
import com.sequenceiq.cloudbreak.service.stack.StackImageService;
import com.sequenceiq.cloudbreak.service.upgrade.sync.operationresult.CmSyncOperationSummary;

@ExtendWith(MockitoExtension.class)
class CmSyncImageUpdateServiceTest {

    private static final long STACK_ID = 1L;

    private static final String CURRENT_IMAGE_ID = "current-image-id";

    private static final String IMAGE_CATALOG_URL = "image-catalog-url";

    private static final String IMAGE_CATALOG_NAME = "image-catalog-name";

    private static final String TARGET_IMAGE_ID = "target-image-id";

    private static final String STACK_VERSION = "7.3.1";

    @InjectMocks
    private CmSyncImageUpdateService underTest;

    @Mock
    private StackImageService stackImageService;

    @Mock
    private StackUpdater stackUpdater;

    @Mock
    private CmSyncImageFinderService cmSyncImageFinderService;

    @Mock
    private CmSyncOperationSummary cmSyncOperationSummary;

    @Test
    void testUpdateImageAfterCmSyncShouldThrowExceptionWhenTheCurrentImageIsNotFound() throws CloudbreakImageNotFoundException {
        when(stackImageService.getCurrentImage(STACK_ID)).thenThrow(new CloudbreakImageNotFoundException("Image not found"));

        assertThrows(CloudbreakServiceException.class, () -> underTest.updateImageAfterCmSync(createStack(), cmSyncOperationSummary, Collections.emptySet()));
    }

    @Test
    void testUpdateImageAfterCmSyncShouldSetNewImageWhenTargetImageIsPresent() throws CloudbreakImageNotFoundException {
        Set<Image> candidateImages = Set.of();
        Stack stack = createStack();
        com.sequenceiq.cloudbreak.cloud.model.Image currentImage = createCurrentImage();
        Image targetImage = createTargetImage(TARGET_IMAGE_ID, true);

        when(stackImageService.getCurrentImage(STACK_ID)).thenReturn(currentImage);
        when(cmSyncImageFinderService.findTargetImageForImageSync(cmSyncOperationSummary, candidateImages, CURRENT_IMAGE_ID, stack.isDatalake()))
                .thenReturn(Optional.of(targetImage));

        underTest.updateImageAfterCmSync(stack, cmSyncOperationSummary, candidateImages);

        ArgumentCaptor<StatedImage> argumentCaptor = ArgumentCaptor.forClass(StatedImage.class);
        verify(stackImageService).replaceStackImageComponent(eq(stack), argumentCaptor.capture(), eq(currentImage));
        StatedImage actualStatedImage = argumentCaptor.getValue();
        assertEquals(TARGET_IMAGE_ID, actualStatedImage.getImage().getUuid());
        assertEquals(IMAGE_CATALOG_NAME, actualStatedImage.getImageCatalogName());
        assertEquals(IMAGE_CATALOG_URL, actualStatedImage.getImageCatalogUrl());
        verify(stackUpdater).updateStackVersion(STACK_ID, STACK_VERSION);
    }

    @Test
    void testUpdateImageAfterCmSyncShouldSetNewImageWhenTargetImageIsPresentNoStackDetails() throws CloudbreakImageNotFoundException {
        Set<Image> candidateImages = Set.of();
        Stack stack = createStack();
        com.sequenceiq.cloudbreak.cloud.model.Image currentImage = createCurrentImage();
        Image targetImage = createTargetImage(TARGET_IMAGE_ID, false);

        when(stackImageService.getCurrentImage(STACK_ID)).thenReturn(currentImage);
        when(cmSyncImageFinderService.findTargetImageForImageSync(cmSyncOperationSummary, candidateImages, CURRENT_IMAGE_ID, stack.isDatalake()))
                .thenReturn(Optional.of(targetImage));

        underTest.updateImageAfterCmSync(stack, cmSyncOperationSummary, candidateImages);

        ArgumentCaptor<StatedImage> argumentCaptor = ArgumentCaptor.forClass(StatedImage.class);
        verify(stackImageService).replaceStackImageComponent(eq(stack), argumentCaptor.capture(), eq(currentImage));
        StatedImage actualStatedImage = argumentCaptor.getValue();
        assertEquals(TARGET_IMAGE_ID, actualStatedImage.getImage().getUuid());
        assertEquals(IMAGE_CATALOG_NAME, actualStatedImage.getImageCatalogName());
        assertEquals(IMAGE_CATALOG_URL, actualStatedImage.getImageCatalogUrl());
        verify(stackUpdater, never()).updateStackVersion(any(), any());
    }

    @Test
    void testUpdateImageAfterCmSyncShouldNotSetNewImageWhenTargetImageIsNotPresent() throws CloudbreakImageNotFoundException {
        Set<Image> candidateImages = Set.of();
        Stack stack = createStack();
        com.sequenceiq.cloudbreak.cloud.model.Image currentImage = createCurrentImage();
        Image targetImage = createTargetImage(CURRENT_IMAGE_ID, false);

        when(stackImageService.getCurrentImage(STACK_ID)).thenReturn(currentImage);
        when(cmSyncImageFinderService.findTargetImageForImageSync(cmSyncOperationSummary, candidateImages, CURRENT_IMAGE_ID, stack.isDatalake()))
                .thenReturn(Optional.of(targetImage));

        underTest.updateImageAfterCmSync(stack, cmSyncOperationSummary, candidateImages);

        verify(stackImageService, never()).replaceStackImageComponent(any(), any(), any());
    }

    @Test
    void testUpdateImageAfterCmSyncShouldNotSetNewImageWhenTargetImageIdIsEqualsWithTheCurrentImage() throws CloudbreakImageNotFoundException {
        Set<Image> candidateImages = Set.of();
        Stack stack = createStack();
        com.sequenceiq.cloudbreak.cloud.model.Image currentImage = createCurrentImage();

        when(stackImageService.getCurrentImage(STACK_ID)).thenReturn(currentImage);
        when(cmSyncImageFinderService.findTargetImageForImageSync(cmSyncOperationSummary, candidateImages, CURRENT_IMAGE_ID, stack.isDatalake()))
                .thenReturn(Optional.empty());

        underTest.updateImageAfterCmSync(stack, cmSyncOperationSummary, candidateImages);

        verify(stackImageService, never()).replaceStackImageComponent(any(), any(), any());
    }

    private Image createTargetImage(String imageId, boolean withImageDetails) {
        Image.ImageBuilder imageBuilder = Image.builder()
                .withUuid(imageId);
        if (withImageDetails) {
            imageBuilder.withStackDetails(new ImageStackDetails(STACK_VERSION, null, null));
        }
        return imageBuilder.build();
    }

    private com.sequenceiq.cloudbreak.cloud.model.Image createCurrentImage() {
        return com.sequenceiq.cloudbreak.cloud.model.Image.builder()
                .withImageId(CURRENT_IMAGE_ID)
                .withImageCatalogUrl(IMAGE_CATALOG_URL)
                .withImageCatalogName(IMAGE_CATALOG_NAME)
                .build();
    }

    private Stack createStack() {
        Stack stack = new Stack();
        stack.setId(STACK_ID);
        stack.setType(WORKLOAD);
        return stack;
    }

}