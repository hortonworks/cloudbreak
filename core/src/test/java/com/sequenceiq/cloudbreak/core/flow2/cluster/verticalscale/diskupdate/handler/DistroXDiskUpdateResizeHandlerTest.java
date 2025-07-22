package com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.diskupdate.handler;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.diskupdate.DistroXDiskUpdateHandlerSelectors.DATAHUB_DISK_RESIZE_HANDLER_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.diskupdate.DistroXDiskUpdateStateSelectors.DATAHUB_DISK_RESIZE_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.diskupdate.DistroXDiskUpdateStateSelectors.FAILED_DATAHUB_DISK_UPDATE_EVENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.DiskType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.DiskUpdateRequest;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.diskupdate.event.DistroXDiskUpdateEvent;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.service.datalake.DiskUpdateService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.common.api.type.ResourceType;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@ExtendWith(MockitoExtension.class)
class DistroXDiskUpdateResizeHandlerTest {

    private static final Long STACK_ID = 1L;

    @Mock
    private StackService stackService;

    @Mock
    private DiskUpdateService diskUpdateService;

    @Mock
    private Stack stack;

    @InjectMocks
    private DistroXDiskUpdateResizeHandler underTest;

    private DistroXDiskUpdateEvent handlerRequest;

    @BeforeEach
    void setUp() {
        doReturn(ResourceType.AWS_VOLUMESET).when(stack).getDiskResourceType();
        doReturn(stack).when(stackService).getByIdWithListsInTransaction(STACK_ID);
        String selector = DATAHUB_DISK_RESIZE_HANDLER_EVENT.selector();
        DiskUpdateRequest updateRequest = new DiskUpdateRequest();
        updateRequest.setGroup("compute");
        updateRequest.setSize(100);
        updateRequest.setVolumeType("GP2");
        handlerRequest = new DistroXDiskUpdateEvent(
                selector,
                STACK_ID,
                "TEST",
                "accountId",
                List.of(),
                "AWS",
                STACK_ID,
                "GP2",
                100,
                "compute",
                DiskType.ADDITIONAL_DISK.name());
    }

    @Test
    void testResizeDisks() throws CloudbreakOrchestratorFailedException {
        Selectable selectable = underTest.doAccept(new HandlerEvent<>(new Event<>(handlerRequest)));
        verify(diskUpdateService, times(1)).resizeDisksAndUpdateFstab(stack, "compute");
        assertEquals(DATAHUB_DISK_RESIZE_FINISHED_EVENT.event(), selectable.getSelector());
    }

    @Test
    void testResizeDisksException() throws CloudbreakOrchestratorFailedException {
        doThrow(new CloudbreakOrchestratorFailedException("TEST")).when(diskUpdateService).resizeDisksAndUpdateFstab(stack, "compute");

        Selectable selectable = underTest.doAccept(new HandlerEvent<>(new Event<>(handlerRequest)));
        assertEquals(FAILED_DATAHUB_DISK_UPDATE_EVENT.selector(), selectable.getSelector());
        verify(diskUpdateService, times(1)).resizeDisksAndUpdateFstab(stack, "compute");
    }

}
