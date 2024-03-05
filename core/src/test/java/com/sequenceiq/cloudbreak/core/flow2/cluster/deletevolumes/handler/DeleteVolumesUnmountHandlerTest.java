package com.sequenceiq.cloudbreak.core.flow2.cluster.deletevolumes.handler;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.deletevolumes.DeleteVolumesEvent.DELETE_VOLUMES_FAIL_HANDLED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.deletevolumes.DeleteVolumesEvent.DELETE_VOLUMES_UNMOUNT_FINISHED_EVENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackDeleteVolumesRequest;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.deletevolumes.DeleteVolumesService;
import com.sequenceiq.cloudbreak.core.flow2.cluster.deletevolumes.DeleteVolumesUnmountEvent;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.service.resource.ResourceService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.common.api.type.ResourceType;
import com.sequenceiq.flow.reactor.api.event.EventSender;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@ExtendWith(MockitoExtension.class)
class DeleteVolumesUnmountHandlerTest {

    private static final Long STACK_ID = 1L;

    @Mock
    private ResourceService resourceService;

    @Mock
    private StackService stackService;

    @Mock
    private DeleteVolumesService deleteVolumesService;

    @Mock
    private EventSender eventSender;

    @InjectMocks
    private DeleteVolumesUnmountHandler underTest;

    private Event<DeleteVolumesUnmountEvent> handlerEvent;

    @Mock
    private Stack stack;

    @BeforeEach
    void setUp() {
        doReturn(stack).when(stackService).getByIdWithLists(STACK_ID);
        doReturn(ResourceType.AWS_VOLUMESET).when(stack).getDiskResourceType();
        CloudResource cloudResource = mock(CloudResource.class);
        Resource resource = mock(Resource.class);
        doReturn("instance-1").when(resource).getInstanceId();
        doReturn(List.of(resource)).when(resourceService).findAllByStackIdAndResourceTypeIn(STACK_ID, List.of(ResourceType.AWS_VOLUMESET));
        StackDeleteVolumesRequest stackDeleteVolumesRequest = mock(StackDeleteVolumesRequest.class);
        handlerEvent = new Event<>(new DeleteVolumesUnmountEvent(STACK_ID, "test", List.of(cloudResource),
                stackDeleteVolumesRequest, "MOCK", Set.of()));
    }

    @Test
    void testMountVolumes() throws Exception {
        Selectable response = underTest.doAccept(new HandlerEvent<>(handlerEvent));
        verify(deleteVolumesService, times(1)).unmountBlockStorageDisks(eq(stack), eq("test"));
        assertEquals(DELETE_VOLUMES_UNMOUNT_FINISHED_EVENT.event(), response.getSelector());
    }

    @Test
    void testMountVolumesException() throws Exception {
        doThrow(new CloudbreakOrchestratorFailedException("TEST")).when(deleteVolumesService).unmountBlockStorageDisks(stack, "test");
        Selectable response = underTest.doAccept(new HandlerEvent<>(handlerEvent));
        verify(deleteVolumesService, times(1)).unmountBlockStorageDisks(eq(stack), eq("test"));
        assertEquals(DELETE_VOLUMES_FAIL_HANDLED_EVENT.event(), response.getSelector());
    }
}
