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
import com.sequenceiq.cloudbreak.cloud.model.catalog.CloudbreakImageCatalogV3;
import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.ComponentConfigProviderService;
import com.sequenceiq.cloudbreak.service.image.ImageCatalogProvider;
import com.sequenceiq.cloudbreak.service.image.ImageProvider;
import com.sequenceiq.cloudbreak.service.upgrade.ImageFilterParamsFactory;

@RunWith(MockitoJUnitRunner.class)
public class LockedComponentServiceTest {

    private static final Long STACK_ID = 3L;

    private static final String TARGET_IMAGE_ID = "target-image-id";

    private static final Map<String, String> ACTIVATED_PARCELS = Collections.emptyMap();

    @InjectMocks
    private LockedComponentService underTest;

    @Mock
    private ImageCatalogProvider imageCatalogProvider;

    @Mock
    private ImageProvider imageProvider;

    @Mock
    private LockedComponentChecker lockedComponentChecker;

    @Mock
    private ImageFilterParamsFactory imageFilterParamsFactory;

    @Mock
    private ComponentConfigProviderService componentConfigProviderService;

    private Stack stack;

    @Before
    public void setup() {
        stack = new Stack();
        stack.setId(STACK_ID);
    }

    @Test
    public void testIsComponentsLockedShouldReturnTrueWhenTheComponentVersionsAreNotMatches()
            throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        Image currentImage = new Image("imageName", Map.of(), "redhat6", "redhat6", "", "default", "default-id", Map.of());
        when(componentConfigProviderService.getImage(STACK_ID)).thenReturn(currentImage);

        CloudbreakImageCatalogV3 imageCatalog = new CloudbreakImageCatalogV3(null, null);
        when(imageCatalogProvider.getImageCatalogV3(currentImage.getImageCatalogUrl())).thenReturn(imageCatalog);

        com.sequenceiq.cloudbreak.cloud.model.catalog.Image currentCatalogImage = mock(com.sequenceiq.cloudbreak.cloud.model.catalog.Image.class);
        when(imageProvider.getCurrentImageFromCatalog(currentImage.getImageId(), imageCatalog)).thenReturn(currentCatalogImage);
        com.sequenceiq.cloudbreak.cloud.model.catalog.Image targetCatalogImage = mock(com.sequenceiq.cloudbreak.cloud.model.catalog.Image.class);
        when(imageProvider.getCurrentImageFromCatalog(TARGET_IMAGE_ID, imageCatalog)).thenReturn(targetCatalogImage);

        when(imageFilterParamsFactory.getStackRelatedParcels(stack)).thenReturn(ACTIVATED_PARCELS);
        when(lockedComponentChecker.isUpgradePermitted(currentCatalogImage, targetCatalogImage, ACTIVATED_PARCELS)).thenReturn(true);

        assertTrue(underTest.isComponentsLocked(stack, TARGET_IMAGE_ID));
        verify(componentConfigProviderService).getImage(STACK_ID);
        verify(imageCatalogProvider).getImageCatalogV3(currentImage.getImageCatalogUrl());
        verify(imageProvider).getCurrentImageFromCatalog(currentImage.getImageId(), imageCatalog);
        verify(imageProvider).getCurrentImageFromCatalog(TARGET_IMAGE_ID, imageCatalog);
        verify(imageFilterParamsFactory).getStackRelatedParcels(stack);
        verify(lockedComponentChecker).isUpgradePermitted(currentCatalogImage, targetCatalogImage, ACTIVATED_PARCELS);
    }

    @Test
    public void testIsComponentsLockedShouldReturnFalseWhenTheComponentVersionsAreMatches()
            throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        Image currentImage = new Image("imageName", Map.of(), "redhat6", "redhat6", "", "default", "default-id", Map.of());
        when(componentConfigProviderService.getImage(STACK_ID)).thenReturn(currentImage);

        CloudbreakImageCatalogV3 imageCatalog = new CloudbreakImageCatalogV3(null, null);
        when(imageCatalogProvider.getImageCatalogV3(currentImage.getImageCatalogUrl())).thenReturn(imageCatalog);

        com.sequenceiq.cloudbreak.cloud.model.catalog.Image currentCatalogImage = mock(com.sequenceiq.cloudbreak.cloud.model.catalog.Image.class);
        when(imageProvider.getCurrentImageFromCatalog(currentImage.getImageId(), imageCatalog)).thenReturn(currentCatalogImage);
        com.sequenceiq.cloudbreak.cloud.model.catalog.Image targetCatalogImage = mock(com.sequenceiq.cloudbreak.cloud.model.catalog.Image.class);
        when(imageProvider.getCurrentImageFromCatalog(TARGET_IMAGE_ID, imageCatalog)).thenReturn(targetCatalogImage);

        when(imageFilterParamsFactory.getStackRelatedParcels(stack)).thenReturn(ACTIVATED_PARCELS);
        when(lockedComponentChecker.isUpgradePermitted(currentCatalogImage, targetCatalogImage, ACTIVATED_PARCELS)).thenReturn(false);

        assertFalse(underTest.isComponentsLocked(stack, TARGET_IMAGE_ID));
        verify(componentConfigProviderService).getImage(STACK_ID);
        verify(imageCatalogProvider).getImageCatalogV3(currentImage.getImageCatalogUrl());
        verify(imageProvider).getCurrentImageFromCatalog(currentImage.getImageId(), imageCatalog);
        verify(imageProvider).getCurrentImageFromCatalog(TARGET_IMAGE_ID, imageCatalog);
        verify(imageFilterParamsFactory).getStackRelatedParcels(stack);
        verify(lockedComponentChecker).isUpgradePermitted(currentCatalogImage, targetCatalogImage, ACTIVATED_PARCELS);
    }

}