package com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes.handler;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes.AddVolumesEvent.ADD_VOLUMES_ORCHESTRATION_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes.AddVolumesEvent.FAILURE_EVENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
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

import com.sequenceiq.cloudbreak.cloud.model.CloudVolumeUsageType;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes.event.AddVolumesFailedEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.addvolumes.event.AddVolumesOrchestrationHandlerEvent;
import com.sequenceiq.cloudbreak.core.flow2.service.AddVolumesService;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.service.diskupdate.DiskUpdateService;
import com.sequenceiq.cloudbreak.service.resource.ResourceService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.common.api.type.ResourceType;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@ExtendWith(MockitoExtension.class)
class AddVolumesOrchestrationHandlerTest {
    private static final Long STACK_ID = 1L;

    @Mock
    private ResourceService resourceService;

    @Mock
    private StackService stackService;

    @Mock
    private DiskUpdateService diskUpdateService;

    @Mock
    private AddVolumesService addVolumesService;

    @InjectMocks
    private AddVolumesOrchestrationHandler underTest;

    private AddVolumesOrchestrationHandlerEvent handlerRequest;

    @Mock
    private Stack stack;

    @BeforeEach
    void setUp() {
        doReturn(ResourceType.AWS_VOLUMESET).when(stack).getDiskResourceType();
        doReturn(stack).when(stackService).getByIdWithLists(STACK_ID);
        Resource resource = mock(Resource.class);
        List<Resource> resources = List.of(resource);
        doReturn(resources).when(resourceService).findAllByStackIdAndInstanceGroupAndResourceTypeIn(eq(STACK_ID), eq("test"),
                    eq(List.of(ResourceType.AWS_VOLUMESET)));

        handlerRequest = new AddVolumesOrchestrationHandlerEvent(STACK_ID, 2L, "gp2", 400L, CloudVolumeUsageType.GENERAL, "test");
    }

    @Test
    void testMountVolumes() throws Exception {
        Map<String, Map<String, String>> fstabInformation = Map.of("test", Map.of("fstab", "test-fstab", "uuid", "123"));
        doReturn(fstabInformation).when(addVolumesService).redeployStatesAndMountDisks(stack, "test");
        Selectable response = underTest.doAccept(new HandlerEvent<>(new Event<>(handlerRequest)));
        assertEquals(ADD_VOLUMES_ORCHESTRATION_FINISHED_EVENT.event(), response.getSelector());
        verify(diskUpdateService, times(1)).parseFstabAndPersistDiskInformation(fstabInformation, stack);
    }

    @Test
    void testMountVolumesException() throws Exception {
        doThrow(new CloudbreakOrchestratorFailedException("TEST")).when(addVolumesService).redeployStatesAndMountDisks(stack, "test");
        Selectable response = underTest.doAccept(new HandlerEvent<>(new Event<>(handlerRequest)));
        assertEquals(FAILURE_EVENT.event(), response.getSelector());
        AddVolumesFailedEvent failedEvent = (AddVolumesFailedEvent) response;
        assertEquals("TEST", failedEvent.getException().getMessage());
    }

}