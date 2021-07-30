package com.sequenceiq.cloudbreak.service.upgrade.sync;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.emptyCollectionOf;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
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
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.image.ImageCatalogService;
import com.sequenceiq.cloudbreak.service.image.ImageService;
import com.sequenceiq.cloudbreak.service.image.StatedImage;
import com.sequenceiq.cloudbreak.structuredevent.LegacyRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;

@ExtendWith(MockitoExtension.class)
public class CmSyncImageCollectorServiceTest {

    private static final String USER_CRN = "userCrn";

    private static final String IMAGE_UUID_1 = "imageUuid1";

    private static final long STACK_ID = 2L;

    private static final String CURRENT_IMAGE_CATALOG_NAME = "currentImageCatalogName";

    private static final long WORKSPCE_ID = 3L;

    private static final String CURRENT_IMAGE_UUID = "currentImageUuid";

    private static final String CURRENT_CLOUD_PLATFORM = "currentCloudPlatform";

    @Mock
    private ImageCatalogService imageCatalogService;

    @Mock
    private ImageService imageService;

    @Mock
    private LegacyRestRequestThreadLocalService legacyRestRequestThreadLocalService;

    @InjectMocks
    private CmSyncImageCollectorService underTest;

    @Mock
    private Stack stack;

    @Test
    void testCollectImagesWhenImageUuidPresentThenCurrentImageAdded() throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        setupStack(true, false);
        Set<String> candidateImageUuids = Set.of(IMAGE_UUID_1);
        StatedImage currentStatedImage = getCurrentStatedImage();
        StatedImage anotherStatedImage = getStatedImage(IMAGE_UUID_1);
        when(imageService.getCurrentImageCatalogName(STACK_ID)).thenReturn(CURRENT_IMAGE_CATALOG_NAME);
        when(imageService.getCurrentImage(STACK_ID)).thenReturn(currentStatedImage);
        when(imageCatalogService.getImageByCatalogName(WORKSPCE_ID, IMAGE_UUID_1, CURRENT_IMAGE_CATALOG_NAME)).thenReturn(anotherStatedImage);

        Set<Image> collectedImages = underTest.collectImages(USER_CRN, stack, candidateImageUuids);

        assertThat(collectedImages, hasSize(2));
        assertThat(collectedImages, containsInAnyOrder(
                hasProperty("uuid", is(IMAGE_UUID_1)),
                hasProperty("uuid", is(CURRENT_IMAGE_UUID))
        ));
        verify(imageService).getCurrentImageCatalogName(STACK_ID);
        verify(imageCatalogService).getImageByCatalogName(WORKSPCE_ID, IMAGE_UUID_1, CURRENT_IMAGE_CATALOG_NAME);
        verify(imageService).getCurrentImage(STACK_ID);
        verify(legacyRestRequestThreadLocalService, never()).setCloudberUserFromUserCrn(anyString());
        verify(imageCatalogService, never()).getCdhImages(anyLong(), anyString(), anyString());
    }

    @Test
    void testCollectImagesWhenNoImageUuidThenAllImagesCollected() throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        setupStack(true, true);
        Set<String> candidateImageUuids = Set.of();
        List<Image> allCdhImages = List.of(getImage(IMAGE_UUID_1));
        when(imageService.getCurrentImageCatalogName(STACK_ID)).thenReturn(CURRENT_IMAGE_CATALOG_NAME);
        when(imageCatalogService.getCdhImages(WORKSPCE_ID, CURRENT_IMAGE_CATALOG_NAME, CURRENT_CLOUD_PLATFORM)).thenReturn(allCdhImages);

        Set<Image> collectedImages = underTest.collectImages(USER_CRN, stack, candidateImageUuids);

        assertThat(collectedImages, hasSize(1));
        assertThat(collectedImages, containsInAnyOrder(
                hasProperty("uuid", is(IMAGE_UUID_1))
        ));
        verify(imageService).getCurrentImageCatalogName(STACK_ID);
        verify(legacyRestRequestThreadLocalService).setCloudberUserFromUserCrn(USER_CRN);
        verify(imageCatalogService).getCdhImages(WORKSPCE_ID, CURRENT_IMAGE_CATALOG_NAME, CURRENT_CLOUD_PLATFORM);
        verify(imageService, never()).getCurrentImage(anyLong());
        verify(imageCatalogService, never()).getImageByCatalogName(anyLong(), anyString(), anyString());
    }

    @Test
    void testCollectImagesWhenImageCatalogNameNotFoundThenReturnsEmpty() throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        setupStack(false, false);
        Set<String> candidateImageUuids = Set.of();
        when(imageService.getCurrentImageCatalogName(STACK_ID)).thenThrow(new CloudbreakImageNotFoundException("The image was not found"));

        Set<Image> collectedImages = underTest.collectImages(USER_CRN, stack, candidateImageUuids);

        assertThat(collectedImages, emptyCollectionOf(Image.class));
        verify(imageService).getCurrentImageCatalogName(STACK_ID);
        verify(legacyRestRequestThreadLocalService, never()).setCloudberUserFromUserCrn(anyString());
        verify(imageCatalogService, never()).getCdhImages(anyLong(), anyString(), anyString());
        verify(imageService, never()).getCurrentImage(anyLong());
        verify(imageCatalogService, never()).getImageByCatalogName(anyLong(), anyString(), anyString());
    }

    @Test
    void testCollectImagesWhenNoImageUuidAndCrnUserNotFoundThenReturnsEmpty() throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        setupStack(true, false);
        Set<String> candidateImageUuids = Set.of();
        when(imageService.getCurrentImageCatalogName(STACK_ID)).thenReturn(CURRENT_IMAGE_CATALOG_NAME);
        doThrow(new UsernameNotFoundException(String.format("User %s not found", USER_CRN)))
                .when(legacyRestRequestThreadLocalService).setCloudberUserFromUserCrn(USER_CRN);

        Set<Image> collectedImages = underTest.collectImages(USER_CRN, stack, candidateImageUuids);

        assertThat(collectedImages, emptyCollectionOf(Image.class));
        verify(imageService).getCurrentImageCatalogName(STACK_ID);
        verify(legacyRestRequestThreadLocalService).setCloudberUserFromUserCrn(USER_CRN);
        verify(imageCatalogService, never()).getCdhImages(anyLong(), anyString(), anyString());
        verify(imageService, never()).getCurrentImage(anyLong());
        verify(imageCatalogService, never()).getImageByCatalogName(anyLong(), anyString(), anyString());
    }

    @Test
    void testCollectImagesWhenNoImageUuidAndImageCatalogExceptionThenReturnsEmpty() throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        setupStack(true, true);
        Set<String> candidateImageUuids = Set.of();
        when(imageCatalogService.getCdhImages(WORKSPCE_ID, CURRENT_IMAGE_CATALOG_NAME, CURRENT_CLOUD_PLATFORM))
                .thenThrow(new CloudbreakImageCatalogException("My custom image catalog exception"));
        when(imageService.getCurrentImageCatalogName(STACK_ID)).thenReturn(CURRENT_IMAGE_CATALOG_NAME);

        Set<Image> collectedImages = underTest.collectImages(USER_CRN, stack, candidateImageUuids);

        assertThat(collectedImages, emptyCollectionOf(Image.class));
        verify(imageService).getCurrentImageCatalogName(STACK_ID);
        verify(legacyRestRequestThreadLocalService).setCloudberUserFromUserCrn(USER_CRN);
        verify(imageCatalogService).getCdhImages(WORKSPCE_ID, CURRENT_IMAGE_CATALOG_NAME, CURRENT_CLOUD_PLATFORM);
        verify(imageService, never()).getCurrentImage(anyLong());
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
        when(image.getUuid()).thenReturn(uuid);
        return image;
    }

    private void setupStack(boolean setupWorkspace, boolean setupCloudPlatform) {
        when(stack.getId()).thenReturn(STACK_ID);
        if (setupWorkspace) {
            Workspace workspace = mock(Workspace.class);
            when(workspace.getId()).thenReturn(WORKSPCE_ID);
            when(stack.getWorkspace()).thenReturn(workspace);
        }
        if (setupCloudPlatform) {
            when(stack.cloudPlatform()).thenReturn(CURRENT_CLOUD_PLATFORM);
        }
    }

}
