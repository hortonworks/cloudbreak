package com.sequenceiq.cloudbreak.service.image;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.CloudbreakImageCatalogV2;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Images;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Versions;
import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@RunWith(MockitoJUnitRunner.class)
public class StackUpgradeImagesServiceTest {

    private static final long WORKSPACE_ID = 1L;

    private static final String STACK_NAME = "test-stack";

    private static final String CATALOG_URL = "/images";

    @InjectMocks
    private StackUpgradeImagesService underTest;

    @Mock
    private StackService stackService;

    @Mock
    private ImageCatalogProvider imageCatalogProvider;

    @Mock
    private ImageService imageService;

    @Mock
    private StackUpgradeImageFilter stackUpgradeImageFilter;

    @Test
    public void testGetImagesToUpgradeShouldReturnsImagesWhenThereAreAvailableImages()
            throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        Stack stack = createStack();
        Image currentImage = createCurrentImage();
        com.sequenceiq.cloudbreak.cloud.model.catalog.Image properImage = Mockito.mock(com.sequenceiq.cloudbreak.cloud.model.catalog.Image.class);
        com.sequenceiq.cloudbreak.cloud.model.catalog.Image otherImage = Mockito.mock(com.sequenceiq.cloudbreak.cloud.model.catalog.Image.class);
        CloudbreakImageCatalogV2 imageCatalog = createImageCatalog(List.of(properImage, otherImage));

        when(stackService.getByNameInWorkspace(STACK_NAME, WORKSPACE_ID)).thenReturn(stack);
        when(imageService.getImage(stack.getId())).thenReturn(currentImage);
        when(imageCatalogProvider.getImageCatalogV2(CATALOG_URL)).thenReturn(imageCatalog);
        when(stackUpgradeImageFilter.filter(imageCatalog.getImages().getCdhImages(), imageCatalog.getVersions(), currentImage, stack.getCloudPlatform()))
                .thenReturn(createFilteredImages(properImage));

        Images actual = underTest.getImagesToUpgrade(WORKSPACE_ID, STACK_NAME);

        assertTrue(actual.getCdhImages().contains(properImage));
        assertEquals(1, actual.getCdhImages().size());
    }

    @Test
    public void testGetImagesToUpgradeShouldReturnsEmptyListWhenCurrentImageIsNotFound() throws CloudbreakImageNotFoundException {
        Stack stack = createStack();
        when(stackService.getByNameInWorkspace(STACK_NAME, WORKSPACE_ID)).thenReturn(stack);
        when(imageService.getImage(stack.getId())).thenThrow(new CloudbreakImageNotFoundException("Image not found."));

        Images actual = underTest.getImagesToUpgrade(WORKSPACE_ID, STACK_NAME);

        assertTrue(actual.getCdhImages().isEmpty());
    }

    private Stack createStack() {
        Stack stack = new Stack();
        stack.setId(2L);
        stack.setCloudPlatform("AWS");
        return stack;
    }

    private Image createCurrentImage() {
        return new Image(null, null, null, null, CATALOG_URL, null, null, null);
    }

    private CloudbreakImageCatalogV2 createImageCatalog(List<com.sequenceiq.cloudbreak.cloud.model.catalog.Image> images) {
        return new CloudbreakImageCatalogV2(new Images(null, null, null, images, null), new Versions(Collections.emptyList()));
    }

    private Images createFilteredImages(com.sequenceiq.cloudbreak.cloud.model.catalog.Image image) {
        return new Images(null, null, null, List.of(image), null);
    }
}