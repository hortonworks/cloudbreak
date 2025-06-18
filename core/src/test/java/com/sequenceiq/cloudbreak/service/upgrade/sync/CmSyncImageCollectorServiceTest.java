package com.sequenceiq.cloudbreak.service.upgrade.sync;

import static com.sequenceiq.common.model.ImageCatalogPlatform.imageCatalogPlatform;
import static com.sequenceiq.common.model.OsType.RHEL8;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.emptyCollectionOf;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.common.service.PlatformStringTransformer;
import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.image.ImageCatalogService;
import com.sequenceiq.cloudbreak.service.image.ImageService;
import com.sequenceiq.cloudbreak.service.image.StatedImage;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.common.model.Architecture;
import com.sequenceiq.common.model.ImageCatalogPlatform;

@ExtendWith(MockitoExtension.class)
public class CmSyncImageCollectorServiceTest {

    private static final String IMAGE_UUID_1 = "imageUuid1";

    private static final long STACK_ID = 2L;

    private static final String CURRENT_IMAGE_CATALOG_NAME = "currentImageCatalogName";

    private static final long WORKSPACE_ID = 3L;

    private static final String CURRENT_IMAGE_UUID = "currentImageUuid";

    private static final String CURRENT_CLOUD_PLATFORM = "currentCloudPlatform";

    private static final String ACCOUNT_ID = "1234";

    private static final String STACK_CRN = "crn:cdp:sdx:us-west-1:1234:stack:mystack";

    @Mock
    private ImageCatalogService imageCatalogService;

    @Mock
    private ImageService imageService;

    @Mock
    private PlatformStringTransformer platformStringTransformer;

    @InjectMocks
    private CmSyncImageCollectorService underTest;

    @Test
    void testCollectImagesWhenImageUuidPresentThenCurrentImageAdded() throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        Stack stack = setupStack(true, false);
        Set<String> candidateImageUuids = Set.of(IMAGE_UUID_1);
        StatedImage currentStatedImage = getCurrentStatedImage();
        StatedImage anotherStatedImage = getStatedImage(IMAGE_UUID_1);
        when(imageService.getCurrentImageCatalogName(STACK_ID)).thenReturn(CURRENT_IMAGE_CATALOG_NAME);
        when(imageService.getCurrentImage(WORKSPACE_ID, STACK_ID)).thenReturn(currentStatedImage);
        when(imageCatalogService.getImageByCatalogName(WORKSPACE_ID, IMAGE_UUID_1, CURRENT_IMAGE_CATALOG_NAME)).thenReturn(anotherStatedImage);

        Set<Image> collectedImages = underTest.collectImages(stack, candidateImageUuids);

        assertThat(collectedImages, hasSize(2));
        assertThat(collectedImages, containsInAnyOrder(
                hasProperty("uuid", is(IMAGE_UUID_1)),
                hasProperty("uuid", is(CURRENT_IMAGE_UUID))
        ));
        verify(imageService).getCurrentImageCatalogName(STACK_ID);
        verify(imageCatalogService).getImageByCatalogName(WORKSPACE_ID, IMAGE_UUID_1, CURRENT_IMAGE_CATALOG_NAME);
        verify(imageService).getCurrentImage(WORKSPACE_ID, STACK_ID);
        verify(imageCatalogService, never()).getAllCdhImages(anyString(), anyLong(), anyString(), anySet());
    }

    @Test
    void testCollectImagesWhenNoImageUuidThenAllImagesCollected() throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        ImageCatalogPlatform imageCatalogPlatform = imageCatalogPlatform(CURRENT_CLOUD_PLATFORM);
        Stack stack = setupStack(true, true);
        Set<String> candidateImageUuids = Set.of();
        List<Image> allCdhImages = List.of(getImage(IMAGE_UUID_1));
        stack.setResourceCrn(STACK_CRN);
        StatedImage currentStatedImage = getCurrentStatedImage();
        when(imageService.getCurrentImage(WORKSPACE_ID, STACK_ID)).thenReturn(currentStatedImage);
        when(imageService.getCurrentImageCatalogName(STACK_ID)).thenReturn(CURRENT_IMAGE_CATALOG_NAME);
        when(imageCatalogService.getAllCdhImages(ACCOUNT_ID, WORKSPACE_ID, CURRENT_IMAGE_CATALOG_NAME, Set.of(imageCatalogPlatform))).thenReturn(allCdhImages);
        when(platformStringTransformer.getPlatformStringForImageCatalogSet(any(), anyString()))
                .thenReturn(Set.of(imageCatalogPlatform));

        Set<Image> collectedImages = underTest.collectImages(stack, candidateImageUuids);

        assertThat(collectedImages, hasSize(1));
        assertThat(collectedImages, containsInAnyOrder(
                hasProperty("uuid", is(IMAGE_UUID_1))
        ));
        verify(imageService).getCurrentImageCatalogName(STACK_ID);
        verify(imageCatalogService).getAllCdhImages(ACCOUNT_ID, WORKSPACE_ID, CURRENT_IMAGE_CATALOG_NAME, Set.of(imageCatalogPlatform));
        verify(imageCatalogService, never()).getImageByCatalogName(anyLong(), anyString(), anyString());
    }

    @Test
    void testCollectImagesWhenArchitectureDoesntMatch() throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        ImageCatalogPlatform imageCatalogPlatform = imageCatalogPlatform(CURRENT_CLOUD_PLATFORM);
        Stack stack = setupStack(true, true);
        Set<String> candidateImageUuids = Set.of();
        List<Image> allCdhImages = List.of(getImage(IMAGE_UUID_1, Architecture.ARM64));
        stack.setResourceCrn(STACK_CRN);
        StatedImage currentStatedImage = getCurrentStatedImage();
        when(imageService.getCurrentImage(WORKSPACE_ID, STACK_ID)).thenReturn(currentStatedImage);
        when(imageService.getCurrentImageCatalogName(STACK_ID)).thenReturn(CURRENT_IMAGE_CATALOG_NAME);
        when(imageCatalogService.getAllCdhImages(ACCOUNT_ID, WORKSPACE_ID, CURRENT_IMAGE_CATALOG_NAME, Set.of(imageCatalogPlatform))).thenReturn(allCdhImages);
        when(platformStringTransformer.getPlatformStringForImageCatalogSet(any(), anyString()))
                .thenReturn(Set.of(imageCatalogPlatform));

        Set<Image> collectedImages = underTest.collectImages(stack, candidateImageUuids);

        assertThat(collectedImages, hasSize(0));
        verify(imageService).getCurrentImageCatalogName(STACK_ID);
        verify(imageCatalogService).getAllCdhImages(ACCOUNT_ID, WORKSPACE_ID, CURRENT_IMAGE_CATALOG_NAME, Set.of(imageCatalogPlatform));
        verify(imageCatalogService, never()).getImageByCatalogName(anyLong(), anyString(), anyString());
    }

    @Test
    void testCollectImagesWhenArmArchitectureMatches() throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        ImageCatalogPlatform imageCatalogPlatform = imageCatalogPlatform(CURRENT_CLOUD_PLATFORM);
        Stack stack = setupStack(true, true);
        Set<String> candidateImageUuids = Set.of();
        List<Image> allCdhImages = List.of(getImage(IMAGE_UUID_1, Architecture.ARM64));
        stack.setResourceCrn(STACK_CRN);
        StatedImage currentStatedImage = getCurrentStatedImage();
        when(currentStatedImage.getImage().getArchitecture()).thenReturn(Architecture.ARM64.getName());
        when(imageService.getCurrentImage(WORKSPACE_ID, STACK_ID)).thenReturn(currentStatedImage);
        when(imageService.getCurrentImageCatalogName(STACK_ID)).thenReturn(CURRENT_IMAGE_CATALOG_NAME);
        when(imageCatalogService.getAllCdhImages(ACCOUNT_ID, WORKSPACE_ID, CURRENT_IMAGE_CATALOG_NAME, Set.of(imageCatalogPlatform))).thenReturn(allCdhImages);
        when(platformStringTransformer.getPlatformStringForImageCatalogSet(any(), anyString()))
                .thenReturn(Set.of(imageCatalogPlatform));

        Set<Image> collectedImages = underTest.collectImages(stack, candidateImageUuids);

        assertThat(collectedImages, hasSize(1));
        assertThat(collectedImages, containsInAnyOrder(
                hasProperty("uuid", is(IMAGE_UUID_1))
        ));
        verify(imageService).getCurrentImageCatalogName(STACK_ID);
        verify(imageCatalogService).getAllCdhImages(ACCOUNT_ID, WORKSPACE_ID, CURRENT_IMAGE_CATALOG_NAME, Set.of(imageCatalogPlatform));
        verify(imageCatalogService, never()).getImageByCatalogName(anyLong(), anyString(), anyString());
    }

    @Test
    void testCollectImagesWhenImageCatalogNameNotFoundThenReturnsEmpty() throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        Stack stack = setupStack(false, false);
        Set<String> candidateImageUuids = Set.of();
        when(imageService.getCurrentImageCatalogName(STACK_ID)).thenThrow(new CloudbreakImageNotFoundException("The image was not found"));

        Set<Image> collectedImages = underTest.collectImages(stack, candidateImageUuids);

        assertThat(collectedImages, emptyCollectionOf(Image.class));
        verify(imageService).getCurrentImageCatalogName(STACK_ID);
        verify(imageCatalogService, never()).getAllCdhImages(anyString(), anyLong(), anyString(), anySet());
        verify(imageService, never()).getCurrentImage(anyLong(), anyLong());
        verify(imageCatalogService, never()).getImageByCatalogName(anyLong(), anyString(), anyString());
    }

    @Test
    void testCollectImagesWhenNoImageUuidAndImageCatalogExceptionThenReturnsEmpty() throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        ImageCatalogPlatform imageCatalogPlatform = imageCatalogPlatform(CURRENT_CLOUD_PLATFORM);
        Stack stack = setupStack(true, true);
        Set<String> candidateImageUuids = Set.of();
        stack.setResourceCrn(STACK_CRN);
        when(imageCatalogService.getAllCdhImages(ACCOUNT_ID, WORKSPACE_ID, CURRENT_IMAGE_CATALOG_NAME, Set.of(imageCatalogPlatform(CURRENT_CLOUD_PLATFORM))))
                .thenThrow(new CloudbreakImageCatalogException("My custom image catalog exception"));
        when(imageService.getCurrentImageCatalogName(STACK_ID)).thenReturn(CURRENT_IMAGE_CATALOG_NAME);
        StatedImage currentStatedImage = getCurrentStatedImage();
        when(imageService.getCurrentImage(WORKSPACE_ID, STACK_ID)).thenReturn(currentStatedImage);
        when(platformStringTransformer.getPlatformStringForImageCatalogSet(any(), anyString()))
                .thenReturn(Set.of(imageCatalogPlatform));

        Set<Image> collectedImages = underTest.collectImages(stack, candidateImageUuids);

        assertThat(collectedImages, emptyCollectionOf(Image.class));
        verify(imageService).getCurrentImageCatalogName(STACK_ID);
        verify(imageCatalogService).getAllCdhImages(ACCOUNT_ID, WORKSPACE_ID, CURRENT_IMAGE_CATALOG_NAME, Set.of(imageCatalogPlatform));
        verify(imageCatalogService, never()).getImageByCatalogName(anyLong(), anyString(), anyString());
    }

    private StatedImage getCurrentStatedImage() {
        return getStatedImage(CURRENT_IMAGE_UUID);
    }

    private StatedImage getStatedImage(String uuid) {
        return StatedImage.statedImage(getImage(uuid), "", CURRENT_IMAGE_CATALOG_NAME);
    }

    private Image getImage(String uuid) {
        Image image = mock(Image.class);
        lenient().when(image.getUuid()).thenReturn(uuid);
        lenient().when(image.getOs()).thenReturn(RHEL8.getOs());
        lenient().when(image.getOsType()).thenReturn(RHEL8.getOsType());
        return image;
    }

    private Image getImage(String uuid, Architecture architecture) {
        Image image = mock(Image.class);
        lenient().when(image.getUuid()).thenReturn(uuid);
        lenient().when(image.getOs()).thenReturn(RHEL8.getOs());
        lenient().when(image.getOsType()).thenReturn(RHEL8.getOsType());
        lenient().when(image.getArchitecture()).thenReturn(architecture.getName());
        return image;
    }

    private Stack setupStack(boolean setupWorkspace, boolean setupCloudPlatform) {
        Stack stack = new Stack();
        stack.setId(STACK_ID);
        if (setupWorkspace) {
            Workspace workspace = new Workspace();
            workspace.setId(WORKSPACE_ID);
            stack.setWorkspace(workspace);
        }
        if (setupCloudPlatform) {
            stack.setCloudPlatform(CURRENT_CLOUD_PLATFORM);
            stack.setPlatformVariant(CURRENT_CLOUD_PLATFORM);
        }
        return stack;
    }

}
