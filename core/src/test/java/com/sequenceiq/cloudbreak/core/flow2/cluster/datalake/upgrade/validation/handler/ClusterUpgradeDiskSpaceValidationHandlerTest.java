package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.handler;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeValidationHandlerSelectors.VALIDATE_DISK_SPACE_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeValidationStateSelectors.FAILED_CLUSTER_UPGRADE_VALIDATION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeValidationStateSelectors.FINISH_CLUSTER_UPGRADE_DISK_SPACE_VALIDATION_EVENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.exception.UpgradeValidationFailedException;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeDiskSpaceValidationEvent;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.upgrade.validation.DiskSpaceValidationService;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@ExtendWith(MockitoExtension.class)
class ClusterUpgradeDiskSpaceValidationHandlerTest {

    private static final long STACK_ID = 1L;

    private static final long REQUIRED_FREE_SPACE = 1024L;

    @InjectMocks
    private ClusterUpgradeDiskSpaceValidationHandler underTest;

    @Mock
    private DiskSpaceValidationService diskSpaceValidationService;

    @Mock
    private StackService stackService;

    @Mock
    private Stack stack;

    @Test
    void testHandlerToRetrieveTheImageFromTheCurrentImageCatalog() {
        when(stackService.getByIdWithListsInTransaction(STACK_ID)).thenReturn(stack);

        Selectable nextFlowStepSelector = underTest.doAccept(createEvent());

        assertEquals(FINISH_CLUSTER_UPGRADE_DISK_SPACE_VALIDATION_EVENT.name(), nextFlowStepSelector.selector());
        verify(stackService).getByIdWithListsInTransaction(STACK_ID);
        verify(diskSpaceValidationService).validateFreeSpaceForUpgrade(stack, REQUIRED_FREE_SPACE);
    }

    @Test
    void testHandlerShouldReturnValidationFailedEventWhenTheDiskValidationFailed() {
        when(stackService.getByIdWithListsInTransaction(STACK_ID)).thenReturn(stack);
        doThrow(new UpgradeValidationFailedException("Validation failed")).when(diskSpaceValidationService)
                .validateFreeSpaceForUpgrade(stack, REQUIRED_FREE_SPACE);

        Selectable nextFlowStepSelector = underTest.doAccept(createEvent());

        assertEquals(FAILED_CLUSTER_UPGRADE_VALIDATION_EVENT.name(), nextFlowStepSelector.selector());
        verify(stackService).getByIdWithListsInTransaction(STACK_ID);
        verify(diskSpaceValidationService).validateFreeSpaceForUpgrade(stack, REQUIRED_FREE_SPACE);
    }

    private HandlerEvent<ClusterUpgradeDiskSpaceValidationEvent> createEvent() {
        return new HandlerEvent<>(new Event<>(new ClusterUpgradeDiskSpaceValidationEvent(VALIDATE_DISK_SPACE_EVENT.selector(), STACK_ID, REQUIRED_FREE_SPACE)));
    }
}