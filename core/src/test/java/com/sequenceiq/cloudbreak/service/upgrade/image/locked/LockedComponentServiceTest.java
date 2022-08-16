package com.sequenceiq.cloudbreak.service.upgrade.image.locked;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.service.ComponentConfigProviderService;
import com.sequenceiq.cloudbreak.service.image.ImageCatalogService;
import com.sequenceiq.cloudbreak.service.image.StatedImage;
import com.sequenceiq.cloudbreak.service.upgrade.ImageFilterParamsFactory;

@RunWith(MockitoJUnitRunner.class)
public class LockedComponentServiceTest {

    private static final Long WORKSPACE_ID = 4L;

    private static final Long STACK_ID = 3L;

    private static final String TARGET_IMAGE_ID = "target-image-id";

    private static final String IMAGE_CATALOG_URL = "image catalog url";

    private static final String IMAGE_CATALOG_NAME = "image catalog name";

    private static final String CURRENT_IMAGE_ID = "current image id";

    private static final Map<String, String> ACTIVATED_PARCELS = Collections.emptyMap();

    @InjectMocks
    private LockedComponentService underTest;

    @Mock
    private LockedComponentChecker lockedComponentChecker;

    @Mock
    private ImageFilterParamsFactory imageFilterParamsFactory;

    @Mock
    private ComponentConfigProviderService componentConfigProviderService;

    @Mock
    private ImageCatalogService imageCatalogService;

    @Mock
    private StackDto stack;

    @Before
    public void setup() {
        stack = mock(StackDto.class);
        when(stack.getId()).thenReturn(STACK_ID);
        when(stack.getWorkspaceId()).thenReturn(WORKSPACE_ID);
    }

    @Test
    public void testIsComponentsLockedShouldReturnTrueWhenTheComponentVersionsAreNotMatches()
            throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        Image currentImage = new Image("imageName", Map.of(), "redhat6", "redhat6", IMAGE_CATALOG_URL, IMAGE_CATALOG_NAME, CURRENT_IMAGE_ID, Map.of());
        when(componentConfigProviderService.getImage(STACK_ID)).thenReturn(currentImage);

        com.sequenceiq.cloudbreak.cloud.model.catalog.Image currentCatalogImage = mock(com.sequenceiq.cloudbreak.cloud.model.catalog.Image.class);
        when(currentCatalogImage.getUuid()).thenReturn(CURRENT_IMAGE_ID);
        when(imageCatalogService.getImage(WORKSPACE_ID, IMAGE_CATALOG_URL, IMAGE_CATALOG_NAME, CURRENT_IMAGE_ID))
                .thenReturn(StatedImage.statedImage(currentCatalogImage, IMAGE_CATALOG_URL, IMAGE_CATALOG_NAME));
        com.sequenceiq.cloudbreak.cloud.model.catalog.Image targetCatalogImage = mock(com.sequenceiq.cloudbreak.cloud.model.catalog.Image.class);
        when(targetCatalogImage.getUuid()).thenReturn(TARGET_IMAGE_ID);
        when(imageCatalogService.getImage(WORKSPACE_ID, IMAGE_CATALOG_URL, IMAGE_CATALOG_NAME, TARGET_IMAGE_ID))
                .thenReturn(StatedImage.statedImage(targetCatalogImage, IMAGE_CATALOG_URL, IMAGE_CATALOG_NAME));
        when(imageFilterParamsFactory.getStackRelatedParcels(stack)).thenReturn(ACTIVATED_PARCELS);
        when(lockedComponentChecker.isUpgradePermitted(currentCatalogImage, targetCatalogImage, ACTIVATED_PARCELS)).thenReturn(true);

        assertTrue(underTest.isComponentsLocked(stack, TARGET_IMAGE_ID));
        verify(componentConfigProviderService).getImage(STACK_ID);
        verify(imageFilterParamsFactory).getStackRelatedParcels(stack);
        verify(lockedComponentChecker).isUpgradePermitted(currentCatalogImage, targetCatalogImage, ACTIVATED_PARCELS);
    }

    @Test
    public void testIsComponentsLockedShouldReturnFalseWhenTheComponentVersionsAreMatches()
            throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        Image currentImage = new Image("imageName", Map.of(), "redhat6", "redhat6", IMAGE_CATALOG_URL, IMAGE_CATALOG_NAME, CURRENT_IMAGE_ID, Map.of());
        when(componentConfigProviderService.getImage(STACK_ID)).thenReturn(currentImage);

        com.sequenceiq.cloudbreak.cloud.model.catalog.Image currentCatalogImage = mock(com.sequenceiq.cloudbreak.cloud.model.catalog.Image.class);
        when(currentCatalogImage.getUuid()).thenReturn(CURRENT_IMAGE_ID);
        when(imageCatalogService.getImage(WORKSPACE_ID, IMAGE_CATALOG_URL, IMAGE_CATALOG_NAME, CURRENT_IMAGE_ID))
                .thenReturn(StatedImage.statedImage(currentCatalogImage, IMAGE_CATALOG_URL, IMAGE_CATALOG_NAME));
        com.sequenceiq.cloudbreak.cloud.model.catalog.Image targetCatalogImage = mock(com.sequenceiq.cloudbreak.cloud.model.catalog.Image.class);
        when(targetCatalogImage.getUuid()).thenReturn(TARGET_IMAGE_ID);
        when(imageCatalogService.getImage(WORKSPACE_ID, IMAGE_CATALOG_URL, IMAGE_CATALOG_NAME, TARGET_IMAGE_ID))
                .thenReturn(StatedImage.statedImage(targetCatalogImage, IMAGE_CATALOG_URL, IMAGE_CATALOG_NAME));

        when(imageFilterParamsFactory.getStackRelatedParcels(stack)).thenReturn(ACTIVATED_PARCELS);
        when(lockedComponentChecker.isUpgradePermitted(currentCatalogImage, targetCatalogImage, ACTIVATED_PARCELS)).thenReturn(false);

        assertFalse(underTest.isComponentsLocked(stack, TARGET_IMAGE_ID));
        verify(componentConfigProviderService).getImage(STACK_ID);
        verify(imageFilterParamsFactory).getStackRelatedParcels(stack);
        verify(lockedComponentChecker).isUpgradePermitted(currentCatalogImage, targetCatalogImage, ACTIVATED_PARCELS);
    }
}