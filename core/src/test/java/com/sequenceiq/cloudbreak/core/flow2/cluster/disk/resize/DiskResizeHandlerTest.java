package com.sequenceiq.cloudbreak.core.flow2.cluster.disk.resize;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.disk.resize.DiskResizeEvent.DISK_RESIZE_HANDLER_EVENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.store.InMemoryStateStore;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.disk.resize.request.DiskResizeHandlerRequest;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.service.diskupdate.DiskUpdateService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.common.api.type.ResourceType;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@ExtendWith(MockitoExtension.class)
public class DiskResizeHandlerTest {

    private static final Long STACK_ID = 1L;

    private static final String INSTANCE_GROUP = "compute";

    @Mock
    private StackService stackService;

    @Mock
    private DiskUpdateService diskUpdateService;

    @InjectMocks
    private DiskResizeHandler underTest;

    private DiskResizeHandlerRequest handlerRequest;

    private Stack stack;

    @BeforeEach
    public void setUp() {
        stack = mock(Stack.class);
        String selector = DISK_RESIZE_HANDLER_EVENT.selector();
        handlerRequest = new DiskResizeHandlerRequest(selector, STACK_ID, INSTANCE_GROUP);
    }

    @Test
    public void testResizeDisks() throws Exception {
        doReturn(ResourceType.AWS_VOLUMESET).when(stack).getDiskResourceType();
        doReturn(stack).when(stackService).getByIdWithListsInTransaction(STACK_ID);

        Selectable response = underTest.doAccept(new HandlerEvent<>(new Event<>(handlerRequest)));

        assertEquals(DiskResizeEvent.DISK_RESIZE_FINISHED_EVENT.event(), response.getSelector());
        assertEquals(STACK_ID, response.getResourceId());
        verify(diskUpdateService, times(1)).resizeDisks(stack, INSTANCE_GROUP);
    }

    @Test
    public void testResizeDisksForGcpDiskset() throws Exception {
        doReturn(ResourceType.GCP_ATTACHED_DISKSET).when(stack).getDiskResourceType();
        doReturn(stack).when(stackService).getByIdWithListsInTransaction(STACK_ID);

        Selectable response = underTest.doAccept(new HandlerEvent<>(new Event<>(handlerRequest)));

        assertEquals(DiskResizeEvent.DISK_RESIZE_FINISHED_EVENT.event(), response.getSelector());
        assertEquals(STACK_ID, response.getResourceId());
        verify(diskUpdateService, times(1)).resizeDisks(stack, INSTANCE_GROUP);
    }

    @Test
    public void testResizeDisksException() throws Exception {
        doReturn(ResourceType.AWS_VOLUMESET).when(stack).getDiskResourceType();
        doReturn(stack).when(stackService).getByIdWithListsInTransaction(STACK_ID);
        doThrow(new CloudbreakOrchestratorFailedException("TEST")).when(diskUpdateService).resizeDisks(stack, INSTANCE_GROUP);

        Selectable response = underTest.doAccept(new HandlerEvent<>(new Event<>(handlerRequest)));

        assertEquals(DiskResizeEvent.FAILURE_EVENT.event(), response.getSelector());
        assertEquals("TEST", response.getException().getMessage());
        assertEquals(CloudbreakOrchestratorFailedException.class, response.getException().getClass());
        assertNull(InMemoryStateStore.getStack(STACK_ID), "InMemoryStateStore entry should be cleared even when resizeDisks throws");
    }

    @Test
    public void testResizeDisksWhenNoDiskResourceType() throws Exception {
        doReturn(null).when(stack).getDiskResourceType();
        doReturn(stack).when(stackService).getByIdWithListsInTransaction(STACK_ID);

        Selectable response = underTest.doAccept(new HandlerEvent<>(new Event<>(handlerRequest)));

        assertEquals(DiskResizeEvent.FAILURE_EVENT.event(), response.getSelector());
        assertEquals(STACK_ID, response.getResourceId());
        verify(diskUpdateService, never()).resizeDisks(stack, INSTANCE_GROUP);
    }
}
