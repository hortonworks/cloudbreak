package com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes.handler;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes.AddVolumesEvent.ATTACH_VOLUMES_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes.AddVolumesEvent.FAILURE_EVENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.CloudVolumeUsageType;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes.event.AttachVolumesHandlerEvent;
import com.sequenceiq.cloudbreak.core.flow2.service.AddVolumesService;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.service.resource.ResourceService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.common.api.type.ResourceType;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@ExtendWith(MockitoExtension.class)
class AttachVolumesHandlerTest {

    private static final Long STACK_ID = 1L;

    @Mock
    private StackService stackService;

    @Mock
    private ResourceService resourceService;

    @Mock
    private AddVolumesService addVolumesService;

    @InjectMocks
    private AttachVolumesHandler underTest;

    private AttachVolumesHandlerEvent handlerRequest;

    @BeforeEach
    void setUp() {
        handlerRequest = new AttachVolumesHandlerEvent(STACK_ID, 2L, "gp2", 400L, CloudVolumeUsageType.GENERAL, "test");
        Stack stack = mock(Stack.class);
        doReturn(stack).when(stackService).getById(eq(STACK_ID));
        doReturn(ResourceType.AWS_VOLUMESET).when(stack).getDiskResourceType();
    }

    @Test
    void testAttachVolumes() {
        Selectable response = underTest.doAccept(new HandlerEvent<>(new Event<>(handlerRequest)));
        assertEquals(ATTACH_VOLUMES_FINISHED_EVENT.event(), response.getSelector());
        verify(addVolumesService).attachVolumes(any(), eq(STACK_ID));
    }

    @Test
    void testAttachVolumesException() throws CloudbreakServiceException {
        doThrow(new CloudbreakServiceException("TEST")).when(addVolumesService).attachVolumes(any(), eq(STACK_ID));
        Selectable response = underTest.doAccept(new HandlerEvent<>(new Event<>(handlerRequest)));
        assertEquals(FAILURE_EVENT.event(), response.getSelector());
        verify(addVolumesService).attachVolumes(any(), eq(STACK_ID));
    }
}
