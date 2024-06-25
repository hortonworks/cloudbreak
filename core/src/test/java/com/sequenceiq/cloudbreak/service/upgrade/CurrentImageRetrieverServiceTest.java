package com.sequenceiq.cloudbreak.service.upgrade;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.image.ImageCatalogService;
import com.sequenceiq.cloudbreak.service.image.ImageService;
import com.sequenceiq.cloudbreak.service.image.ModelImageTestBuilder;
import com.sequenceiq.cloudbreak.service.image.StatedImage;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;

@ExtendWith(MockitoExtension.class)
class CurrentImageRetrieverServiceTest {

    private static final String IMAGE_NAME = "image-name";

    private static final String IMAGE_CATALOG_URL = "image-catalog-url";

    private static final String IMAGE_CATALOG_NAME = "image-catalog-name";

    private static final String IMAGE_ID = "image-id";

    private static final long WORKSPACE_ID = 2L;

    @Mock
    private ImageCatalogService imageCatalogService;

    @Mock
    private ImageService imageService;

    @InjectMocks
    private CurrentImageRetrieverService underTest;

    private final Stack stack = createStack();

    @Test
    void testRetrieveCurrentModelImage() throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        Image currentImage = createCurrentImage();
        com.sequenceiq.cloudbreak.cloud.model.catalog.Image catalogImage = createCatalogImage();
        when(imageService.getImage(stack.getId())).thenReturn(currentImage);
        when(imageCatalogService.getImage(WORKSPACE_ID, IMAGE_CATALOG_URL, IMAGE_CATALOG_NAME, IMAGE_ID))
                .thenReturn(StatedImage.statedImage(catalogImage, IMAGE_CATALOG_URL, IMAGE_CATALOG_NAME));

        Image actual = underTest.retrieveCurrentModelImage(stack);

        assertEquals(IMAGE_NAME, actual.getImageName());
        assertEquals(currentImage.getUserdata(), actual.getUserdata());
        assertEquals(catalogImage.getOs(), actual.getOs());
        assertEquals(catalogImage.getOsType(), actual.getOsType());
        assertEquals(IMAGE_CATALOG_URL, actual.getImageCatalogUrl());
        assertEquals(IMAGE_CATALOG_NAME, actual.getImageCatalogName());
        assertEquals(IMAGE_ID, actual.getImageId());
        assertEquals(catalogImage.getPackageVersions(), actual.getPackageVersions());
        assertEquals(catalogImage.getDate(), actual.getDate());
        assertEquals(catalogImage.getCreated(), actual.getCreated());
        verify(imageService).getImage(stack.getId());
        verify(imageCatalogService).getImage(WORKSPACE_ID, IMAGE_CATALOG_URL, IMAGE_CATALOG_NAME, IMAGE_ID);
    }

    @Test
    void testRetrieveCurrentModelImageShouldReturnExceptionWhenTheCurrentImageIsNotFoundInTheDatabase() throws CloudbreakImageNotFoundException {
        when(imageService.getImage(stack.getId())).thenThrow(new CloudbreakImageNotFoundException("image not found"));

        assertThrows(CloudbreakImageNotFoundException.class, () -> underTest.retrieveCurrentModelImage(stack));

        verify(imageService).getImage(stack.getId());
        verifyNoInteractions(imageCatalogService);
    }

    @Test
    void testRetrieveCurrentModelImageShouldReturnCurrentImageFromDbWhenTheImageIsNotPresentInTheImageCatalog()
            throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        Image currentImage = createCurrentImage();
        when(imageService.getImage(stack.getId())).thenReturn(currentImage);
        when(imageCatalogService.getImage(WORKSPACE_ID, IMAGE_CATALOG_URL, IMAGE_CATALOG_NAME, IMAGE_ID))
                .thenThrow(new CloudbreakImageNotFoundException("image not found"));

        Image actual = underTest.retrieveCurrentModelImage(stack);

        assertEquals(IMAGE_NAME, actual.getImageName());
        assertEquals(currentImage.getUserdata(), actual.getUserdata());
        assertEquals(currentImage.getOs(), actual.getOs());
        assertEquals(currentImage.getOsType(), actual.getOsType());
        assertEquals(IMAGE_CATALOG_URL, actual.getImageCatalogUrl());
        assertEquals(IMAGE_CATALOG_NAME, actual.getImageCatalogName());
        assertEquals(IMAGE_ID, actual.getImageId());
        assertEquals(currentImage.getPackageVersions(), actual.getPackageVersions());
        assertEquals(currentImage.getDate(), actual.getDate());
        assertEquals(currentImage.getCreated(), actual.getCreated());
        verify(imageService).getImage(stack.getId());
        verify(imageCatalogService).getImage(WORKSPACE_ID, IMAGE_CATALOG_URL, IMAGE_CATALOG_NAME, IMAGE_ID);
    }

    private Stack createStack() {
        Stack stack = new Stack();
        stack.setId(1L);
        Workspace workspace = new Workspace();
        workspace.setId(WORKSPACE_ID);
        stack.setWorkspace(workspace);
        return stack;
    }

    private Image createCurrentImage() {
        return ModelImageTestBuilder.builder()
                .withImageName("image-name")
                .withUserData(Collections.emptyMap())
                .withOs("centos")
                .withOsType("centos")
                .withImageCatalogUrl(IMAGE_CATALOG_URL)
                .withImageCatalogName(IMAGE_CATALOG_NAME)
                .withImageId(IMAGE_ID)
                .withPackageVersions(Collections.emptyMap())
                .withCreated(12345L)
                .withDate("1953-02-21")
                .build();
    }

    private com.sequenceiq.cloudbreak.cloud.model.catalog.Image createCatalogImage() {
        return com.sequenceiq.cloudbreak.cloud.model.catalog.Image.builder()
                .withOs("redhat")
                .withOsType("redhat")
                .withUuid(IMAGE_ID)
                .withPackageVersions(Map.of("stack", "7.2.14"))
                .withCreated(123456L)
                .withDate("1673-11-12")
                .build();
    }
}