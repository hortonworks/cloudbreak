package com.sequenceiq.cloudbreak.core.flow2.cluster.deletevolumes.handler;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.deletevolumes.DeleteVolumesEvent.DELETE_VOLUMES_ORCHESTRATION_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.deletevolumes.DeleteVolumesEvent.FAIL_HANDLED_EVENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cluster.util.ResourceAttributeUtil;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.deletevolumes.DeleteVolumesOrchestrationEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.deletevolumes.DeleteVolumesService;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.service.resource.ResourceService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.flow.reactor.api.event.EventSender;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@ExtendWith(MockitoExtension.class)
class DeleteVolumesOrchestrationHandlerTest {

    private static final Long STACK_ID = 1L;

    @Mock
    private ResourceService resourceService;

    @Mock
    private StackService stackService;

    @Mock
    private ResourceAttributeUtil resourceAttributeUtil;

    @Mock
    private DeleteVolumesService deleteVolumesService;

    @Mock
    private EventSender eventSender;

    @InjectMocks
    private DeleteVolumesOrchestrationHandler underTest;

    private Event<DeleteVolumesOrchestrationEvent> handlerEvent;

    @Mock
    private Stack stack;

    @BeforeEach
    void setUp() {
        doReturn(stack).when(stackService).getByIdWithLists(STACK_ID);
        handlerEvent = new Event<>(new DeleteVolumesOrchestrationEvent(STACK_ID, "test"));
    }

    @Test
    void testMountVolumes() throws Exception {
        InstanceMetaData instanceMetaData = mock(InstanceMetaData.class);
        doReturn("test").when(instanceMetaData).getDiscoveryFQDN();
        doReturn("instance-1").when(instanceMetaData).getInstanceId();
        doReturn(List.of(instanceMetaData)).when(stack).getInstanceMetaDataAsList();
        Resource resource = mock(Resource.class);
        doReturn("instance-1").when(resource).getInstanceId();
        doReturn(List.of(resource)).when(stack).getDiskResources();
        Map<String, Map<String, String>> fstabInformation = Map.of("test", Map.of("fstab", "test-fstab", "uuids", "123"));
        doReturn(fstabInformation).when(deleteVolumesService).redeployStatesAndMountDisks(stack, "test");
        Selectable response = underTest.doAccept(new HandlerEvent<>(handlerEvent));
        verify(deleteVolumesService, times(1)).redeployStatesAndMountDisks(eq(stack), eq("test"));
        verify(resourceService).saveAll(List.of(resource));
        assertEquals(DELETE_VOLUMES_ORCHESTRATION_FINISHED_EVENT.event(), response.getSelector());
    }

    @Test
    void testMountVolumesResourceNotUpdated() throws Exception {
        InstanceMetaData instanceMetaData = mock(InstanceMetaData.class);
        doReturn("test").when(instanceMetaData).getDiscoveryFQDN();
        doReturn("instance-1").when(instanceMetaData).getInstanceId();
        doReturn(List.of(instanceMetaData)).when(stack).getInstanceMetaDataAsList();
        Map<String, Map<String, String>> fstabInformation = Map.of("test", Map.of("fstab", "test-fstab"));
        doReturn(fstabInformation).when(deleteVolumesService).redeployStatesAndMountDisks(stack, "test");
        Selectable response = underTest.doAccept(new HandlerEvent<>(handlerEvent));
        verify(deleteVolumesService, times(1)).redeployStatesAndMountDisks(eq(stack), eq("test"));
        verify(resourceService, times(0)).saveAll(anyList());
        assertEquals(DELETE_VOLUMES_ORCHESTRATION_FINISHED_EVENT.event(), response.getSelector());
    }

    @Test
    void testMountVolumesException() throws Exception {
        doThrow(new CloudbreakOrchestratorFailedException("TEST")).when(deleteVolumesService).redeployStatesAndMountDisks(stack, "test");
        Selectable response = underTest.doAccept(new HandlerEvent<>(handlerEvent));
        verify(deleteVolumesService, times(1)).redeployStatesAndMountDisks(eq(stack), eq("test"));
        assertEquals(FAIL_HANDLED_EVENT.event(), response.getSelector());
    }
}
