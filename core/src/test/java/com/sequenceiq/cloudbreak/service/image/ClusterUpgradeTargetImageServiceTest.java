package com.sequenceiq.cloudbreak.service.image;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.type.ComponentType;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.domain.stack.Component;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.ComponentConfigProviderService;
import com.sequenceiq.cloudbreak.service.stack.StackImageService;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@ExtendWith(MockitoExtension.class)
public class ClusterUpgradeTargetImageServiceTest {

    private static final long STACK_ID = 1L;

    private static final String IMAGE_ID = "image-id";

    private static final String TARGET_IMAGE = "TARGET_IMAGE";

    private static final String STACK_VERSION = "7.2.3";

    private static final String IMAGE_CATALOG_URL = "image-catalog-url";

    private static final String IMAGE_CATALOG_NAME = "image-catalog-name";

    @InjectMocks
    private ClusterUpgradeTargetImageService underTest;

    @Mock
    private ComponentConfigProviderService componentConfigProviderService;

    @Mock
    private StackService stackService;

    @Mock
    private StackImageService stackImageService;

    @Mock
    private ImageProvider imageProvider;

    @Mock
    private Stack stack;

    @Mock
    private StatedImage targetStatedImage;

    @Mock
    private Image currentImage;

    @Test
    void testSaveImageShouldSaveTheTargetImageWhenIsNotPresent() throws IOException, CloudbreakImageNotFoundException {
        Image targetModelImage = createTargetModelImage(IMAGE_ID, IMAGE_CATALOG_URL, IMAGE_CATALOG_NAME);
        when(stackImageService.findImageComponentByName(STACK_ID, TARGET_IMAGE)).thenReturn(Optional.empty());
        when(stackService.getById(STACK_ID)).thenReturn(stack);
        when(stackImageService.getCurrentImage(stack)).thenReturn(currentImage);
        when(stackImageService.getImageModelFromStatedImage(stack, currentImage, targetStatedImage)).thenReturn(targetModelImage);

        underTest.saveImage(STACK_ID, targetStatedImage);

        verify(stackImageService).findImageComponentByName(STACK_ID, TARGET_IMAGE);
        verify(stackService).getById(STACK_ID);
        ArgumentCaptor<Component> targetComponentCaptor = ArgumentCaptor.forClass(Component.class);
        verify(componentConfigProviderService).store(targetComponentCaptor.capture());
        Component targetComponent = targetComponentCaptor.getValue();
        assertEquals(ComponentType.IMAGE, targetComponent.getComponentType());
        assertEquals(TARGET_IMAGE, targetComponent.getName());
        assertEquals(stack, targetComponent.getStack());
        assertEquals(targetModelImage.getImageId(), targetComponent.getAttributes().get(Image.class).getImageId());
    }

    @Test
    void testSaveImageShouldRemoveTheOldTargetImageAndSaveTheTargetImageWhenAnOldImageIsPresent() throws IOException, CloudbreakImageNotFoundException {
        StatedImage targetImage = ImageTestUtil.getImageFromCatalog(true, IMAGE_ID, STACK_VERSION, IMAGE_CATALOG_URL, IMAGE_CATALOG_NAME);
        Image targetModelImage = createTargetModelImage(IMAGE_ID, IMAGE_CATALOG_URL, IMAGE_CATALOG_NAME);
        Image oldTargetImage = createTargetModelImage("old-image-id", IMAGE_CATALOG_URL, IMAGE_CATALOG_NAME);
        Optional<Component> existingTargetImageComponent = createExistingTargetImageComponent(oldTargetImage);
        when(imageProvider.convertJsonToImage(existingTargetImageComponent.get().getAttributes())).thenReturn(oldTargetImage);
        when(stackImageService.findImageComponentByName(STACK_ID, TARGET_IMAGE)).thenReturn(existingTargetImageComponent);
        when(stackService.getById(STACK_ID)).thenReturn(stack);
        when(stackImageService.getCurrentImage(stack)).thenReturn(currentImage);
        when(stackImageService.getImageModelFromStatedImage(stack, currentImage, targetImage)).thenReturn(targetModelImage);

        underTest.saveImage(STACK_ID, targetImage);

        verify(stackImageService).getCurrentImage(stack);
        verify(stackImageService).findImageComponentByName(STACK_ID, TARGET_IMAGE);
        verify(stackService).getById(STACK_ID);
        ArgumentCaptor<Component> targetComponentCaptor = ArgumentCaptor.forClass(Component.class);
        verify(componentConfigProviderService).store(targetComponentCaptor.capture());
        Component targetComponent = targetComponentCaptor.getValue();
        assertEquals(ComponentType.IMAGE, targetComponent.getComponentType());
        assertEquals(TARGET_IMAGE, targetComponent.getName());
        assertEquals(stack, targetComponent.getStack());
        assertEquals(targetModelImage.getImageId(), targetComponent.getAttributes().get(Image.class).getImageId());
    }

    @Test
    void testSaveImageShouldRemoveTheOldTargetImageAndSaveTheTargetImageWhenAnOldImageIsPresentAndTheImageCatalogNameIsNotTheSame()
            throws IOException, CloudbreakImageNotFoundException {
        StatedImage targetImage = ImageTestUtil.getImageFromCatalog(true, IMAGE_ID, STACK_VERSION, IMAGE_CATALOG_URL, IMAGE_CATALOG_NAME);
        Image targetModelImage = createTargetModelImage(IMAGE_ID, IMAGE_CATALOG_URL, IMAGE_CATALOG_NAME);
        Image oldTargetImage = createTargetModelImage(IMAGE_ID, "old-image-catalog-url", IMAGE_CATALOG_NAME);
        Optional<Component> existingTargetImageComponent = createExistingTargetImageComponent(oldTargetImage);
        when(imageProvider.convertJsonToImage(existingTargetImageComponent.get().getAttributes())).thenReturn(oldTargetImage);
        when(stackImageService.findImageComponentByName(STACK_ID, TARGET_IMAGE)).thenReturn(existingTargetImageComponent);
        when(stackService.getById(STACK_ID)).thenReturn(stack);
        when(stackImageService.getCurrentImage(stack)).thenReturn(currentImage);
        when(stackImageService.getImageModelFromStatedImage(stack, currentImage, targetImage)).thenReturn(targetModelImage);

        underTest.saveImage(STACK_ID, targetImage);

        verify(stackImageService).getCurrentImage(stack);
        verify(stackImageService).findImageComponentByName(STACK_ID, TARGET_IMAGE);
        verify(stackService).getById(STACK_ID);
        ArgumentCaptor<Component> targetComponentCaptor = ArgumentCaptor.forClass(Component.class);
        verify(componentConfigProviderService).store(targetComponentCaptor.capture());
        Component targetComponent = targetComponentCaptor.getValue();
        assertEquals(ComponentType.IMAGE, targetComponent.getComponentType());
        assertEquals(TARGET_IMAGE, targetComponent.getName());
        assertEquals(stack, targetComponent.getStack());
        assertEquals(targetModelImage.getImageId(), targetComponent.getAttributes().get(Image.class).getImageId());
    }

    @Test
    void testSaveImageShouldNotSaveTheTargetImageWhenIsAlreadyPresent() {
        StatedImage targetImage = ImageTestUtil.getImageFromCatalog(true, IMAGE_ID, STACK_VERSION, IMAGE_CATALOG_URL, IMAGE_CATALOG_NAME);
        Image targetModelImageImage = createTargetModelImage(IMAGE_ID, IMAGE_CATALOG_URL, IMAGE_CATALOG_NAME);
        Optional<Component> existingTargetImageComponent = createExistingTargetImageComponent(targetModelImageImage);
        when(stackImageService.findImageComponentByName(STACK_ID, TARGET_IMAGE)).thenReturn(existingTargetImageComponent);
        when(imageProvider.convertJsonToImage(existingTargetImageComponent.get().getAttributes())).thenReturn(targetModelImageImage);

        underTest.saveImage(STACK_ID, targetImage);

        verify(stackImageService).findImageComponentByName(STACK_ID, TARGET_IMAGE);
        verify(imageProvider).convertJsonToImage(existingTargetImageComponent.get().getAttributes());
        verifyNoInteractions(stackService);
        verifyNoInteractions(componentConfigProviderService);
    }

    private Optional<Component> createExistingTargetImageComponent(Image image) {
        return Optional.of(new Component(ComponentType.IMAGE, TARGET_IMAGE, new Json(image), stack));
    }

    private Image createTargetModelImage(String imageId, String imageCatalogUrl, String imageCatalogName) {
        return new Image(null, Collections.emptyMap(), null, null, imageCatalogUrl, imageCatalogName, imageId, new HashMap<>());
    }

}