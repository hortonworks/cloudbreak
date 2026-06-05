package com.sequenceiq.cloudbreak.core.flow2.cluster.disksync.handler;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.disksync.DiskSyncEvent.DISK_SYNC_PROCESS_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.disksync.DiskSyncEvent.FAILURE_EVENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.DiskSyncMode;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.core.flow2.cluster.disksync.request.DiskSyncHandlerEvent;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.job.disk.DiskSyncService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@ExtendWith(MockitoExtension.class)
class DiskSyncHandlerTest {

    private static final Long STACK_ID = 1L;

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private DiskSyncService diskSyncService;

    @InjectMocks
    private DiskSyncHandler underTest;

    private DiskSyncHandlerEvent handlerRequest;

    @BeforeEach
    void setUp() {
        handlerRequest = new DiskSyncHandlerEvent(STACK_ID, DiskSyncMode.DRY_RUN);
    }

    @Test
    void testDiskSync() {
        StackDto stackDto = mock(StackDto.class);
        when(stackDtoService.getById(STACK_ID)).thenReturn(stackDto);

        Selectable response = underTest.doAccept(new HandlerEvent<>(new Event<>(handlerRequest)));

        assertEquals(DISK_SYNC_PROCESS_FINISHED_EVENT.event(), response.getSelector());
        verify(diskSyncService).syncResources(eq(stackDto), eq(DiskSyncMode.DRY_RUN));
    }

    @Test
    void testDiskSyncException() {
        StackDto stackDto = mock(StackDto.class);
        when(stackDtoService.getById(STACK_ID)).thenReturn(stackDto);
        doThrow(new CloudbreakServiceException("TEST")).when(diskSyncService).syncResources(eq(stackDto), eq(DiskSyncMode.PERSIST));

        Selectable response = underTest.doAccept(new HandlerEvent<>(new Event<>(handlerRequest)));

        assertEquals(FAILURE_EVENT.event(), response.getSelector());
        verify(diskSyncService).syncResources(eq(stackDto), eq(DiskSyncMode.DRY_RUN));
    }
}
