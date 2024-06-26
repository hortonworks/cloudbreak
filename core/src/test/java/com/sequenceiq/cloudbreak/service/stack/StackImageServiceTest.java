package com.sequenceiq.cloudbreak.service.stack;

import static com.sequenceiq.common.model.ImageCatalogPlatform.imageCatalogPlatform;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.security.internal.InternalCrnModifier;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.service.PlatformStringTransformer;
import com.sequenceiq.cloudbreak.common.type.ComponentType;
import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.domain.ImageCatalog;
import com.sequenceiq.cloudbreak.domain.stack.Component;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.ComponentConfigProviderService;
import com.sequenceiq.cloudbreak.service.image.ImageCatalogService;
import com.sequenceiq.cloudbreak.service.image.ImageService;
import com.sequenceiq.cloudbreak.service.image.StatedImage;
import com.sequenceiq.cloudbreak.service.image.userdata.UserDataService;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.common.model.ImageCatalogPlatform;

@ExtendWith(MockitoExtension.class)
public class StackImageServiceTest {

    private static final Long WORKSPACE_ID = 123L;

    private static final String IMAGE_ID = "image id";

    private static final String IMAGE_NAME = "image name";

    private static final String SOURCE_IMAGE_CATALOG = "source image catalog";

    private static final String TARGET_IMAGE_CATALOG = "target image catalog";

    private static final String TARGET_IMAGE_CATALOG_URL = "target image catalog url";

    private static final String TARGET_IMAGE = "TARGET_IMAGE";

    private Stack stack;

    private Image image;

    private StatedImage statedImage;

    private final Map<String, String> packageVersions = Collections.singletonMap("package", "version");

    @Mock
    private ComponentConfigProviderService componentConfigProviderService;

    @Mock
    private ImageService imageService;

    @Mock
    private UserDataService userDataService;

    @Mock
    private ImageCatalogService imageCatalogService;

    @Mock
    private InternalCrnModifier internalCrnModifier;

    @Mock
    private PlatformStringTransformer platformStringTransformer;

    @Mock
    private StackDtoService stackDtoService;

    @Captor
    private ArgumentCaptor<Component> componentArgumentCaptor;

    @InjectMocks
    private StackImageService victim;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        Workspace workspace = new Workspace();
        workspace.setId(WORKSPACE_ID);

        stack = new Stack();
        stack.setId(1L);
        stack.setName("stackname");
        stack.setRegion("region");
        stack.setCloudPlatform("AWS");
        stack.setWorkspace(workspace);
        stack.setResourceCrn("crn:cdp:datahub:us-west-1:accountId:cluster:name");

        image = anImage("uuid");
        statedImage = StatedImage.statedImage(image, "url", "name");
    }

    @Test
    public void testStoreNewImageComponent() throws CloudbreakImageNotFoundException, IOException {

        com.sequenceiq.cloudbreak.cloud.model.Image imageInComponent = com.sequenceiq.cloudbreak.cloud.model.Image.builder()
                .withImageName("imageOldName")
                .withOs(image.getOs())
                .withOsType(image.getOsType())
                .withImageCatalogUrl(statedImage.getImageCatalogUrl())
                .withImageCatalogName(statedImage.getImageCatalogName())
                .withImageId("uuid2").withPackageVersions(packageVersions)
                .build();
        when(componentConfigProviderService.getImage(anyLong())).thenReturn(imageInComponent);
        when(platformStringTransformer.getPlatformStringForImageCatalog(stack.getCloudPlatform(), stack.getPlatformVariant()))
                .thenReturn(imageCatalogPlatform(stack.getCloudPlatform()));
        when(imageService.determineImageName(anyString(), any(), anyString(), eq(image))).thenReturn(IMAGE_NAME);

        victim.storeNewImageComponent(stack, statedImage);

        ArgumentCaptor<Component> captor = ArgumentCaptor.forClass(Component.class);
        verify(componentConfigProviderService).replaceImageComponentWithNew(captor.capture());
        verify(userDataService, times(1)).makeSureUserDataIsMigrated(anyLong());
        assertEquals(ComponentType.IMAGE, captor.getValue().getComponentType());
        assertEquals(ComponentType.IMAGE.name(), captor.getValue().getName());
        assertEquals(IMAGE_NAME, captor.getValue().getAttributes().get(com.sequenceiq.cloudbreak.cloud.model.Image.class).getImageName());
        assertEquals(image.getUuid(), captor.getValue().getAttributes().get(com.sequenceiq.cloudbreak.cloud.model.Image.class).getImageId());
    }

    @Test
    public void testChangeImageCatalogOutOfFlow() throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException, IOException {
        ImageCatalog targetImageCatalog = mock(ImageCatalog.class);
        ImageCatalog sourceImageCatalog = mock(ImageCatalog.class);
        ImageCatalogPlatform imageCatalogPlatform = imageCatalogPlatform(stack.getCloudPlatform());

        Image targetImage = anImage(IMAGE_ID);
        StatedImage targetStatedImage = StatedImage.statedImage(targetImage, TARGET_IMAGE_CATALOG_URL, TARGET_IMAGE_CATALOG);

        when(componentConfigProviderService.getImage(stack.getId())).thenReturn(anImageComponent());
        when(imageCatalogService.getImageCatalogByName(stack.getWorkspace().getId(), SOURCE_IMAGE_CATALOG)).thenReturn(sourceImageCatalog);
        when(imageCatalogService.getImageCatalogByName(stack.getWorkspace().getId(), TARGET_IMAGE_CATALOG)).thenReturn(targetImageCatalog);
        when(imageCatalogService.isCustomImageCatalog(sourceImageCatalog)).thenReturn(false);
        when(imageCatalogService.isCustomImageCatalog(targetImageCatalog)).thenReturn(false);
        when(targetImageCatalog.getName()).thenReturn(TARGET_IMAGE_CATALOG);
        when(targetImageCatalog.getImageCatalogUrl()).thenReturn(TARGET_IMAGE_CATALOG_URL);
        when(imageCatalogService.getImage(WORKSPACE_ID, TARGET_IMAGE_CATALOG_URL, TARGET_IMAGE_CATALOG, IMAGE_ID)).thenReturn(targetStatedImage);
        when(imageService.determineImageName(stack.getCloudPlatform().toLowerCase(Locale.ROOT), imageCatalogPlatform,
                stack.getRegion(), targetStatedImage.getImage())).thenReturn(IMAGE_NAME);
        when(platformStringTransformer.getPlatformStringForImageCatalog(stack.getCloudPlatform(), stack.getPlatformVariant()))
                .thenReturn(imageCatalogPlatform);

        victim.changeImageCatalog(stack, TARGET_IMAGE_CATALOG);

        verify(componentConfigProviderService).replaceImageComponentWithNew(componentArgumentCaptor.capture());
        com.sequenceiq.cloudbreak.cloud.model.Image newImage
                = componentArgumentCaptor.getValue().getAttributes().get(com.sequenceiq.cloudbreak.cloud.model.Image.class);
        assertEquals(TARGET_IMAGE_CATALOG, newImage.getImageCatalogName());
        assertEquals(TARGET_IMAGE_CATALOG_URL, newImage.getImageCatalogUrl());
        assertEquals(IMAGE_ID, newImage.getImageId());
        verify(userDataService, times(1)).makeSureUserDataIsMigrated(anyLong());
    }

    @Test
    public void testChangeImageCatalogOutOfFlowThrowsNotFoundExceptionInCaseOfMissingTargetCatalog()
            throws CloudbreakImageNotFoundException {
        ImageCatalog sourceImageCatalog = mock(ImageCatalog.class);

        when(componentConfigProviderService.getImage(stack.getId())).thenReturn(anImageComponent());
        when(imageCatalogService.getImageCatalogByName(stack.getWorkspace().getId(), SOURCE_IMAGE_CATALOG)).thenReturn(sourceImageCatalog);
        when(imageCatalogService.isCustomImageCatalog(sourceImageCatalog)).thenReturn(false);
        when(imageCatalogService.getImageCatalogByName(stack.getWorkspace().getId(), TARGET_IMAGE_CATALOG)).thenThrow(new NotFoundException(""));

        assertThrows(NotFoundException.class, () -> victim.changeImageCatalog(stack, TARGET_IMAGE_CATALOG));
        verify(userDataService, times(0)).makeSureUserDataIsMigrated(anyLong());
    }

    @Test
    public void testChangeImageCatalogOutOfFlowThrowsNotFoundExceptionInCaseOfImageMissingFromTargetCatalog()
            throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        ImageCatalog targetImageCatalog = mock(ImageCatalog.class);
        ImageCatalog sourceImageCatalog = mock(ImageCatalog.class);

        when(componentConfigProviderService.getImage(stack.getId())).thenReturn(anImageComponent());
        when(imageCatalogService.getImageCatalogByName(stack.getWorkspace().getId(), TARGET_IMAGE_CATALOG)).thenReturn(targetImageCatalog);
        when(imageCatalogService.getImageCatalogByName(stack.getWorkspace().getId(), SOURCE_IMAGE_CATALOG)).thenReturn(sourceImageCatalog);
        when(imageCatalogService.isCustomImageCatalog(sourceImageCatalog)).thenReturn(false);
        when(imageCatalogService.isCustomImageCatalog(targetImageCatalog)).thenReturn(false);
        when(targetImageCatalog.getName()).thenReturn(TARGET_IMAGE_CATALOG);
        when(targetImageCatalog.getImageCatalogUrl()).thenReturn(TARGET_IMAGE_CATALOG_URL);
        when(imageCatalogService.getImage(WORKSPACE_ID, TARGET_IMAGE_CATALOG_URL, TARGET_IMAGE_CATALOG, IMAGE_ID))
                .thenThrow(new CloudbreakImageNotFoundException(""));

        assertThrows(NotFoundException.class, () -> victim.changeImageCatalog(stack, TARGET_IMAGE_CATALOG));
        verify(userDataService, times(0)).makeSureUserDataIsMigrated(anyLong());
    }

    @Test
    public void testChangeImageCatalogOutOfFlowThrowsCloudbreakServiceExceptionInCaseOfImageCatalogException()
            throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        ImageCatalog targetImageCatalog = mock(ImageCatalog.class);
        ImageCatalog sourceImageCatalog = mock(ImageCatalog.class);

        when(componentConfigProviderService.getImage(stack.getId())).thenReturn(anImageComponent());
        when(imageCatalogService.getImageCatalogByName(stack.getWorkspace().getId(), TARGET_IMAGE_CATALOG)).thenReturn(targetImageCatalog);
        when(imageCatalogService.getImageCatalogByName(stack.getWorkspace().getId(), SOURCE_IMAGE_CATALOG)).thenReturn(sourceImageCatalog);
        when(imageCatalogService.isCustomImageCatalog(sourceImageCatalog)).thenReturn(false);
        when(imageCatalogService.isCustomImageCatalog(targetImageCatalog)).thenReturn(false);
        when(targetImageCatalog.getName()).thenReturn(TARGET_IMAGE_CATALOG);
        when(targetImageCatalog.getImageCatalogUrl()).thenReturn(TARGET_IMAGE_CATALOG_URL);
        when(imageCatalogService.getImage(WORKSPACE_ID, TARGET_IMAGE_CATALOG_URL, TARGET_IMAGE_CATALOG, IMAGE_ID))
                .thenThrow(new CloudbreakImageCatalogException(""));

        assertThrows(CloudbreakServiceException.class, () -> victim.changeImageCatalog(stack, TARGET_IMAGE_CATALOG));
    }

    @Test
    void testRemoveTargetImageShouldRemoveImageWhenTheImagePresent() {
        Component targetImageComponent = createImageComponent();
        when(componentConfigProviderService.getComponent(stack.getId(), ComponentType.IMAGE, TARGET_IMAGE)).thenReturn(targetImageComponent);

        victim.removeImageByComponentName(stack.getId(), TARGET_IMAGE);

        verify(componentConfigProviderService).getComponent(stack.getId(), ComponentType.IMAGE, TARGET_IMAGE);
        verify(componentConfigProviderService).deleteComponents(Collections.singleton(targetImageComponent));
    }

    @Test
    void testRemoveTargetImageShouldNotRemoveImageWhenTheImageIsNotPresent() {
        when(componentConfigProviderService.getComponent(stack.getId(), ComponentType.IMAGE, TARGET_IMAGE)).thenReturn(null);

        victim.removeImageByComponentName(stack.getId(), TARGET_IMAGE);

        verify(componentConfigProviderService).getComponent(stack.getId(), ComponentType.IMAGE, TARGET_IMAGE);
        verifyNoMoreInteractions(componentConfigProviderService);
    }

    @Test
    public void testGetStatedImageForStackInternal() throws Exception {
        ImageCatalog imageCatalog = mock(ImageCatalog.class);
        com.sequenceiq.cloudbreak.cloud.model.Image imageComp = mock(com.sequenceiq.cloudbreak.cloud.model.Image.class);
        when(imageCatalog.getName()).thenReturn(TARGET_IMAGE_CATALOG);
        when(imageComp.getImageCatalogName()).thenReturn(TARGET_IMAGE_CATALOG);
        when(imageComp.getImageId()).thenReturn(IMAGE_ID);
        when(componentConfigProviderService.getImage(anyLong())).thenReturn(imageComp);
        when(internalCrnModifier.getInternalCrnWithAccountId(anyString())).thenReturn("accountId");
        when(imageCatalogService.getImageCatalogByName(anyLong(), anyString())).thenReturn(imageCatalog);
        when(imageCatalogService.getImageByCatalogName(anyLong(), anyString(), anyString())).thenReturn(statedImage);

        Optional<StatedImage> result = victim.getStatedImageInternal(stack);

        assertTrue(result.isPresent());
    }

    @Test
    public void testCustomImageCatalogIsNotSupportedToChangeTheExistingCatalog() throws Exception {
        ImageCatalog targetImageCatalog = mock(ImageCatalog.class);
        ImageCatalog sourceImageCatalog = mock(ImageCatalog.class);

        when(componentConfigProviderService.getImage(stack.getId())).thenReturn(anImageComponent());
        when(imageCatalogService.getImageCatalogByName(stack.getWorkspace().getId(), SOURCE_IMAGE_CATALOG)).thenReturn(sourceImageCatalog);
        when(imageCatalogService.getImageCatalogByName(stack.getWorkspace().getId(), TARGET_IMAGE_CATALOG)).thenReturn(targetImageCatalog);
        when(imageCatalogService.isCustomImageCatalog(sourceImageCatalog)).thenReturn(false);
        when(imageCatalogService.isCustomImageCatalog(targetImageCatalog)).thenReturn(true);

        assertThrows(CloudbreakServiceException.class, () -> victim.changeImageCatalog(stack, TARGET_IMAGE_CATALOG));

        verifyNoMoreInteractions(componentConfigProviderService);
    }

    @Test
    public void testCustomImageCatalogIsNotSupportedToBeChanged() throws Exception {
        ImageCatalog sourceImageCatalog = mock(ImageCatalog.class);

        when(componentConfigProviderService.getImage(stack.getId())).thenReturn(anImageComponent());
        when(imageCatalogService.getImageCatalogByName(stack.getWorkspace().getId(), SOURCE_IMAGE_CATALOG)).thenReturn(sourceImageCatalog);
        when(imageCatalogService.isCustomImageCatalog(sourceImageCatalog)).thenReturn(true);

        assertThrows(CloudbreakServiceException.class, () -> victim.changeImageCatalog(stack, TARGET_IMAGE_CATALOG));

        verifyNoMoreInteractions(componentConfigProviderService);
    }

    private Component createImageComponent() {
        com.sequenceiq.cloudbreak.cloud.model.Image targetModelImage = anImageComponent();
        return new Component(ComponentType.IMAGE, TARGET_IMAGE, new Json(targetModelImage), stack);
    }

    private Image anImage(String imageId) {
        return Image.builder()
                .withUuid(imageId)
                .withImageSetsByProvider(Collections.singletonMap("AWS", Collections.emptyMap()))
                .withPackageVersions(packageVersions)
                .build();
    }

    private com.sequenceiq.cloudbreak.cloud.model.Image anImageComponent() {
        return com.sequenceiq.cloudbreak.cloud.model.Image.builder()
                .withImageName("imagename")
                .withImageCatalogName(SOURCE_IMAGE_CATALOG)
                .withImageId(IMAGE_ID)
                .build();
    }
}