package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.handler;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeValidationHandlerSelectors.VALIDATE_DISK_SPACE_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeValidationStateSelectors.FAILED_CLUSTER_UPGRADE_VALIDATION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeValidationStateSelectors.FINISH_CLUSTER_UPGRADE_DISK_SPACE_VALIDATION_EVENT;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.exception.UpgradeValidationFailedException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeValidationEvent;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.image.ImageService;
import com.sequenceiq.cloudbreak.service.image.ImageTestUtil;
import com.sequenceiq.cloudbreak.service.image.StatedImage;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.upgrade.validation.DiskSpaceValidationService;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

import reactor.bus.Event;

@RunWith(MockitoJUnitRunner.class)
public class ClusterUpgradeDiskSpaceValidationHandlerTest {

    private static final String IMAGE_ID = "fe2ad098-88e9-4ffd-b1fa-780a2d353dc5";

    private static final long STACK_ID = 1L;

    private static final String IMAGE_CATALOG_NAME = "test-catalog";

    private static final String IMAGE_CATALOG_URL = "catalog-url";

    @InjectMocks
    private ClusterUpgradeDiskSpaceValidationHandler underTest;

    @Mock
    private DiskSpaceValidationService diskSpaceValidationService;

    @Mock
    private ImageService imageService;

    @Mock
    private StackService stackService;

    @Mock
    private Stack stack;

    @Test
    public void testHandlerToRetrieveTheImageFromTheCurrentImageCatalog()
            throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException, CloudbreakException {
        StatedImage targetImage = createStatedImage();
        when(imageService.getCurrentImage(STACK_ID)).thenReturn(targetImage);
        when(stackService.getByIdWithListsInTransaction(STACK_ID)).thenReturn(stack);

        Selectable nextFlowStepSelector = underTest.doAccept(createEvent());

        assertEquals(FINISH_CLUSTER_UPGRADE_DISK_SPACE_VALIDATION_EVENT.name(), nextFlowStepSelector.selector());
        verify(imageService).getCurrentImage(STACK_ID);
        verify(stackService).getByIdWithListsInTransaction(STACK_ID);
        verify(diskSpaceValidationService).validateFreeSpaceForUpgrade(stack, targetImage);
    }

    @Test
    public void testHandlerShouldReturnFinishEventWhenImageServiceThrowsAnException() throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        when(imageService.getCurrentImage(STACK_ID)).thenThrow(new CloudbreakImageNotFoundException("Image not found"));

        Selectable nextFlowStepSelector = underTest.doAccept(createEvent());

        assertEquals(FINISH_CLUSTER_UPGRADE_DISK_SPACE_VALIDATION_EVENT.name(), nextFlowStepSelector.selector());
        verify(imageService).getCurrentImage(STACK_ID);
        verifyNoInteractions(stackService);
        verifyNoInteractions(diskSpaceValidationService);
    }

    @Test
    public void testHandlerShouldReturnValidationFailedEventWhenTheDiskValidationFailed()
            throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException, CloudbreakException {
        StatedImage targetImage = createStatedImage();
        when(imageService.getCurrentImage(STACK_ID)).thenReturn(targetImage);
        when(stackService.getByIdWithListsInTransaction(STACK_ID)).thenReturn(stack);
        doThrow(new UpgradeValidationFailedException("Validation failed")).when(diskSpaceValidationService).validateFreeSpaceForUpgrade(stack, targetImage);

        Selectable nextFlowStepSelector = underTest.doAccept(createEvent());

        assertEquals(FAILED_CLUSTER_UPGRADE_VALIDATION_EVENT.name(), nextFlowStepSelector.selector());
        verify(imageService).getCurrentImage(STACK_ID);
        verify(stackService).getByIdWithListsInTransaction(STACK_ID);
        verify(diskSpaceValidationService).validateFreeSpaceForUpgrade(stack, targetImage);
    }

    private HandlerEvent<ClusterUpgradeValidationEvent> createEvent() {
        return new HandlerEvent<>(new Event<>(new ClusterUpgradeValidationEvent(VALIDATE_DISK_SPACE_EVENT.selector(), STACK_ID, IMAGE_ID)));
    }

    private StatedImage createStatedImage() {
        return StatedImage.statedImage(ImageTestUtil.getImage(false, IMAGE_ID, null), IMAGE_CATALOG_URL, IMAGE_CATALOG_NAME);
    }

}