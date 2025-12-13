package com.sequenceiq.cloudbreak.service.image;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.CloudbreakImageCatalogV3;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.stack.StackImageService;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;

@ExtendWith(MockitoExtension.class)
public class GenerateImageCatalogServiceTest {

    private static final Long WORKSPACE_ID = 1L;

    private static final String IMAGE_CATALOG_NAME = "image catalog name";

    private static final String IMAGE_CATALOG_URL = "image catalog url";

    private static final String IMAGE_ID = "image catalog id";

    private static final String SOURCE_IMAGE_ID = "source image id";

    private static final long STACK_ID = 1L;

    @Mock
    private StackImageService stackImageService;

    @Mock
    private ImageCatalogService imageCatalogService;

    @InjectMocks
    private GenerateImageCatalogService victim;

    @Mock
    private Stack stack;

    @Mock
    private Image image;

    @Mock
    private StatedImage statedImage;

    @Mock
    private Workspace workspace;

    @Mock
    private com.sequenceiq.cloudbreak.cloud.model.catalog.Image catalogImage;

    @Test
    public void shouldGenerateImageCatalog() throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        when(stack.getId()).thenReturn(1L);
        when(stackImageService.getCurrentImage(STACK_ID)).thenReturn(image);
        when(image.getImageCatalogUrl()).thenReturn(IMAGE_CATALOG_URL);
        when(image.getImageCatalogName()).thenReturn(IMAGE_CATALOG_NAME);
        when(image.getImageId()).thenReturn(IMAGE_ID);
        when(stack.getWorkspace()).thenReturn(workspace);
        when(workspace.getId()).thenReturn(WORKSPACE_ID);
        when(imageCatalogService.getImage(WORKSPACE_ID, IMAGE_CATALOG_URL, IMAGE_CATALOG_NAME, IMAGE_ID)).thenReturn(statedImage);
        when(statedImage.getImage()).thenReturn(catalogImage);

        CloudbreakImageCatalogV3 actual = victim.generateImageCatalogForStack(stack);

        assertThat(actual.getImages().getCdhImages()).first()
                .returns(catalogImage.getCreated(), com.sequenceiq.cloudbreak.cloud.model.catalog.Image::getCreated)
                .returns(catalogImage.getDate(), com.sequenceiq.cloudbreak.cloud.model.catalog.Image::getDate)
                .returns(catalogImage.getDescription(), com.sequenceiq.cloudbreak.cloud.model.catalog.Image::getDescription)
                .returns(catalogImage.getOs(), com.sequenceiq.cloudbreak.cloud.model.catalog.Image::getOs)
                .returns(catalogImage.getUuid(), com.sequenceiq.cloudbreak.cloud.model.catalog.Image::getUuid)
                .returns(catalogImage.getRepo(), com.sequenceiq.cloudbreak.cloud.model.catalog.Image::getRepo)
                .returns(catalogImage.getImageSetsByProvider(), com.sequenceiq.cloudbreak.cloud.model.catalog.Image::getImageSetsByProvider)
                .returns(catalogImage.getStackDetails(), com.sequenceiq.cloudbreak.cloud.model.catalog.Image::getStackDetails)
                .returns(catalogImage.getOsType(), com.sequenceiq.cloudbreak.cloud.model.catalog.Image::getOsType)
                .returns(catalogImage.getPackageVersions(), com.sequenceiq.cloudbreak.cloud.model.catalog.Image::getPackageVersions)
                .returns(catalogImage.getPreWarmParcels(), com.sequenceiq.cloudbreak.cloud.model.catalog.Image::getPreWarmParcels)
                .returns(catalogImage.getPreWarmCsd(), com.sequenceiq.cloudbreak.cloud.model.catalog.Image::getPreWarmCsd)
                .returns(catalogImage.getCmBuildNumber(), com.sequenceiq.cloudbreak.cloud.model.catalog.Image::getCmBuildNumber)
                .returns(true, com.sequenceiq.cloudbreak.cloud.model.catalog.Image::isAdvertised);
        assertThat(actual.getImages().getSupportedVersions())
                .isEmpty();
        assertThat(actual.getVersions())
                .isNull();
    }

    @Test
    public void shouldThrowCloudbreakServiceExceptionInCaseOfImageCatalogUrlIsNull() throws CloudbreakImageNotFoundException {
        when(stack.getId()).thenReturn(STACK_ID);
        when(stackImageService.getCurrentImage(STACK_ID)).thenReturn(image);
        when(image.getImageCatalogUrl()).thenReturn(null);

        assertThrows(CloudbreakServiceException.class, () -> victim.generateImageCatalogForStack(stack));
    }

    @Test
    public void shouldThrowCloudbreakServiceExceptionInCaseOfCustomImage() throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        when(stack.getId()).thenReturn(STACK_ID);
        when(stackImageService.getCurrentImage(STACK_ID)).thenReturn(image);
        when(image.getImageCatalogUrl()).thenReturn(IMAGE_CATALOG_URL);
        when(image.getImageCatalogName()).thenReturn(IMAGE_CATALOG_NAME);
        when(image.getImageId()).thenReturn(IMAGE_ID);
        when(stack.getWorkspace()).thenReturn(workspace);
        when(workspace.getId()).thenReturn(WORKSPACE_ID);
        when(imageCatalogService.getImage(WORKSPACE_ID, IMAGE_CATALOG_URL, IMAGE_CATALOG_NAME, IMAGE_ID)).thenReturn(statedImage);
        when(statedImage.getImage()).thenReturn(catalogImage);
        when(catalogImage.getSourceImageId()).thenReturn(SOURCE_IMAGE_ID);

        assertThrows(CloudbreakServiceException.class, () -> victim.generateImageCatalogForStack(stack));
    }

    @Test
    public void shouldMapCloudbreakImageNotFoundExceptionToNotFoundException() throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        when(stack.getId()).thenReturn(STACK_ID);
        when(stackImageService.getCurrentImage(STACK_ID)).thenReturn(image);
        when(image.getImageCatalogUrl()).thenReturn(IMAGE_CATALOG_URL);
        when(image.getImageCatalogName()).thenReturn(IMAGE_CATALOG_NAME);
        when(image.getImageId()).thenReturn(IMAGE_ID);
        when(stack.getWorkspace()).thenReturn(workspace);
        when(workspace.getId()).thenReturn(WORKSPACE_ID);
        when(imageCatalogService.getImage(WORKSPACE_ID, IMAGE_CATALOG_URL, IMAGE_CATALOG_NAME, IMAGE_ID)).thenThrow(new CloudbreakImageNotFoundException(""));

        assertThrows(NotFoundException.class, () -> victim.generateImageCatalogForStack(stack));
    }

    @Test
    public void shouldMapCloudbreakCloudbreakImageCatalogExceptionToCloudbreakServiceException()
            throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        when(stack.getId()).thenReturn(STACK_ID);
        when(stackImageService.getCurrentImage(STACK_ID)).thenReturn(image);
        when(image.getImageCatalogUrl()).thenReturn(IMAGE_CATALOG_URL);
        when(image.getImageCatalogName()).thenReturn(IMAGE_CATALOG_NAME);
        when(image.getImageId()).thenReturn(IMAGE_ID);
        when(stack.getWorkspace()).thenReturn(workspace);
        when(workspace.getId()).thenReturn(WORKSPACE_ID);
        when(imageCatalogService.getImage(WORKSPACE_ID, IMAGE_CATALOG_URL, IMAGE_CATALOG_NAME, IMAGE_ID)).thenThrow(new CloudbreakImageCatalogException(""));

        assertThrows(CloudbreakServiceException.class, () -> victim.generateImageCatalogForStack(stack));
    }
}