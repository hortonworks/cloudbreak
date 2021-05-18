package com.sequenceiq.cloudbreak.service.upgrade;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.service.ComponentConfigProviderService;
import com.sequenceiq.cloudbreak.service.image.ImageCatalogService;
import com.sequenceiq.cloudbreak.service.image.StatedImage;

@ExtendWith(MockitoExtension.class)
public class UpgradeImageInfoFactoryTest {

    private static final long STACK_ID = 1L;

    private static final String CURRENT_IMAGE_ID = "currentImageId";

    private static final String TARGET_IMAGE_ID = "targetImageId";

    private static final String IMAGE_CATALOG_NAME = "imageCatalogName";

    private static final String IMAGE_CATALOG_URL = "imageCatalogUrl";

    @Mock
    private ComponentConfigProviderService componentConfigProviderService;

    @Mock
    private ImageCatalogService imageCatalogService;

    @InjectMocks
    private UpgradeImageInfoFactory underTest;

    @Test
    void testCreate() throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        Image image = getImage();
        when(componentConfigProviderService.getImage(STACK_ID)).thenReturn(image);
        StatedImage currentStatedImage = StatedImage.statedImage(getCatalogImage(), IMAGE_CATALOG_NAME, CURRENT_IMAGE_ID);
        when(imageCatalogService.getImage(IMAGE_CATALOG_URL, IMAGE_CATALOG_NAME, CURRENT_IMAGE_ID)).thenReturn(currentStatedImage);
        StatedImage targetStatedImage = StatedImage.statedImage(getCatalogImage(), IMAGE_CATALOG_NAME, TARGET_IMAGE_ID);
        when(imageCatalogService.getImage(IMAGE_CATALOG_URL, IMAGE_CATALOG_NAME, TARGET_IMAGE_ID)).thenReturn(targetStatedImage);

        UpgradeImageInfo upgradeImageInfo = underTest.create(TARGET_IMAGE_ID, STACK_ID);

        assertEquals(image, upgradeImageInfo.getCurrentImage());
        assertEquals(currentStatedImage, upgradeImageInfo.getCurrentStatedImage());
        assertEquals(targetStatedImage, upgradeImageInfo.getTargetStatedImage());
        verify(image).getImageId();
        verify(image, times(2)).getImageCatalogName();
        verify(image, times(2)).getImageCatalogUrl();
    }

    private Image getImage() {
        Image image = mock(Image.class);
        when(image.getImageId()).thenReturn(CURRENT_IMAGE_ID);
        when(image.getImageCatalogName()).thenReturn(IMAGE_CATALOG_NAME);
        when(image.getImageCatalogUrl()).thenReturn(IMAGE_CATALOG_URL);
        return image;
    }

    private com.sequenceiq.cloudbreak.cloud.model.catalog.Image getCatalogImage() {
        return mock(com.sequenceiq.cloudbreak.cloud.model.catalog.Image.class);
    }

}
