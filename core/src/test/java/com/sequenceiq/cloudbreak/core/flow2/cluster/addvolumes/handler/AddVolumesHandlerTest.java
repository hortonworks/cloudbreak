package com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes.handler;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes.AddVolumesEvent.ADD_VOLUMES_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes.AddVolumesEvent.FAILURE_EVENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.CloudVolumeUsageType;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes.event.AddVolumesHandlerEvent;
import com.sequenceiq.cloudbreak.core.flow2.service.AddVolumesService;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.service.resource.ResourceService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.common.api.type.ResourceType;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@ExtendWith(MockitoExtension.class)
class AddVolumesHandlerTest {

    private static final Long STACK_ID = 1L;

    @Mock
    private StackService stackService;

    @Mock
    private AddVolumesService addVolumesService;

    @Mock
    private ResourceService resourceService;

    @InjectMocks
    private AddVolumesHandler underTest;

    private AddVolumesHandlerEvent handlerRequest;

    @BeforeEach
    void setUp() {
        handlerRequest = new AddVolumesHandlerEvent(STACK_ID, 2L, "gp2", 400L, CloudVolumeUsageType.GENERAL, "test");
        Stack stack = mock(Stack.class);
        doReturn(stack).when(stackService).getById(eq(STACK_ID));
        doReturn(ResourceType.AWS_VOLUMESET).when(stack).getDiskResourceType();
    }

    @Test
    void testAddVolumes() {
        Resource resource = mock(Resource.class);
        doReturn(List.of(resource)).when(addVolumesService).createVolumes(anySet(), any(), anyInt(), anyString(), any());
        Selectable response = underTest.doAccept(new HandlerEvent<>(new Event<>(handlerRequest)));
        assertEquals(ADD_VOLUMES_FINISHED_EVENT.event(), response.getSelector());
    }

    @Test
    void testAddVolumesException() throws CloudbreakServiceException {
        doThrow(new CloudbreakServiceException("TEST")).when(addVolumesService).createVolumes(anySet(), any(), anyInt(), anyString(), any());
        Selectable response = underTest.doAccept(new HandlerEvent<>(new Event<>(handlerRequest)));
        assertEquals(FAILURE_EVENT.event(), response.getSelector());
        verify(addVolumesService).createVolumes(anySet(), any(), anyInt(), anyString(), any());
    }
}
