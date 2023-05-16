package com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.diskupdate.handler;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.diskupdate.DistroXDiskUpdateHandlerSelectors.DATAHUB_DISK_RESIZE_HANDLER_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.diskupdate.DistroXDiskUpdateStateSelectors.DATAHUB_DISK_RESIZE_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.diskupdate.DistroXDiskUpdateStateSelectors.FAILED_DATAHUB_DISK_UPDATE_EVENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.DiskUpdateRequest;
import com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.diskupdate.event.DistroXDiskUpdateEvent;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.service.datalake.DiskUpdateService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.common.api.type.ResourceType;
import com.sequenceiq.flow.reactor.api.event.BaseFailedFlowEvent;
import com.sequenceiq.flow.reactor.api.event.BaseFlowEvent;
import com.sequenceiq.flow.reactor.api.event.EventSender;

@ExtendWith(MockitoExtension.class)
class DistroXDiskUpdateResizeHandlerTest {

    private static final Long STACK_ID = 1L;

    @Mock
    private StackService stackService;

    @Mock
    private DiskUpdateService diskUpdateService;

    @Mock
    private EventSender eventSender;

    @Mock
    private Stack stack;

    private DistroXDiskUpdateResizeHandler underTest;

    private DistroXDiskUpdateEvent handlerRequest;

    @Captor
    private ArgumentCaptor<BaseFlowEvent> captor;

    @Captor
    private ArgumentCaptor<BaseFailedFlowEvent> failedCaptor;

    @BeforeEach
    void setUp() {
        underTest = new DistroXDiskUpdateResizeHandler(eventSender);
        ReflectionTestUtils.setField(underTest, null, diskUpdateService, DiskUpdateService.class);
        ReflectionTestUtils.setField(underTest, null, stackService, StackService.class);
        doReturn(ResourceType.AWS_VOLUMESET).when(stack).getDiskResourceType();
        doReturn(stack).when(stackService).getByIdWithListsInTransaction(STACK_ID);
        String selector = DATAHUB_DISK_RESIZE_HANDLER_EVENT.selector();
        DiskUpdateRequest updateRequest = new DiskUpdateRequest();
        updateRequest.setGroup("compute");
        updateRequest.setSize(100);
        updateRequest.setVolumeType("GP2");
        handlerRequest = new DistroXDiskUpdateEvent(selector, STACK_ID, "TEST_CRN", "TEST", "accountId", updateRequest,
                List.of(), "AWS", STACK_ID);
    }

    @Test
    void testResizeDisks() throws CloudbreakOrchestratorFailedException {
        ReflectionTestUtils.setField(underTest, null, eventSender, EventSender.class);
        underTest.accept(new Event<>(handlerRequest));
        verify(diskUpdateService, times(1)).resizeDisksAndUpdateFstab(stack, "compute");
        verify(eventSender, times(1)).sendEvent(captor.capture(), any());
        assertEquals(DATAHUB_DISK_RESIZE_FINISHED_EVENT.event(), captor.getValue().getSelector());
    }

    @Test
    void testResizeDisksException() throws CloudbreakOrchestratorFailedException {
        doThrow(new CloudbreakOrchestratorFailedException("TEST")).when(diskUpdateService).resizeDisksAndUpdateFstab(stack, "compute");

        underTest.accept(new Event<>(handlerRequest));
        verify(eventSender, times(1)).sendEvent(failedCaptor.capture(), any());
        assertEquals(FAILED_DATAHUB_DISK_UPDATE_EVENT.selector(), failedCaptor.getValue().getSelector());
        verify(diskUpdateService, times(1)).resizeDisksAndUpdateFstab(stack, "compute");
    }

}
