package com.sequenceiq.cloudbreak.core.flow2.cluster.disk.resize;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.disk.resize.DiskResizeEvent.DISK_RESIZE_HANDLER_EVENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anySet;
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

import com.sequenceiq.cloudbreak.cloud.model.Volume;
import com.sequenceiq.cloudbreak.cluster.util.ResourceAttributeUtil;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.core.flow2.cluster.disk.resize.request.DiskResizeHandlerRequest;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.datalake.DiskUpdateService;
import com.sequenceiq.cloudbreak.service.resource.ResourceService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.util.StackUtil;
import com.sequenceiq.common.api.type.ResourceType;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@ExtendWith(MockitoExtension.class)
public class DiskResizeHandlerTest {

    private static final Long STACK_ID = 1L;

    @Mock
    private StackService stackService;

    @Mock
    private ResourceService resourceService;

    @Mock
    private StackUtil stackUtil;

    @Mock
    private HostOrchestrator hostOrchestrator;

    @Mock
    private GatewayConfigService gatewayConfigService;

    @Mock
    private ResourceAttributeUtil resourceAttributeUtil;

    @Mock
    private DiskUpdateService diskUpdateService;

    @InjectMocks
    private DiskResizeHandler underTest;

    private DiskResizeHandlerRequest handlerRequest;

    @BeforeEach
    public void setUp() {
        Stack stack = mock(Stack.class);
        doReturn(ResourceType.AWS_VOLUMESET).when(stack).getDiskResourceType();
        doReturn(stack).when(stackService).getByIdWithListsInTransaction(STACK_ID);
        Set<Node> allNodes = Set.of(mock(Node.class));
        doReturn(allNodes).when(stackUtil).collectNodes(stack);
        Cluster cluster = mock(Cluster.class);
        doReturn(cluster).when(stack).getCluster();
        String selector = DISK_RESIZE_HANDLER_EVENT.selector();
        List<Volume> volumesToUpdate = List.of(mock(Volume.class));
        handlerRequest = new DiskResizeHandlerRequest(
                selector,
                STACK_ID,
                "compute",
                "GP3",
                500,
                volumesToUpdate);
    }

    @Test
    public void testResizeDisks() throws Exception {
        Selectable response = underTest.doAccept(new HandlerEvent<>(new Event<>(handlerRequest)));
        assertEquals(DiskResizeEvent.DISK_RESIZE_FINISHED_EVENT.event(), response.getSelector());
        assertEquals(STACK_ID, response.getResourceId());
        verify(diskUpdateService, times(1)).updateDiskTypeAndSize(
                handlerRequest.getInstanceGroup(),
                handlerRequest.getVolumeType(),
                handlerRequest.getSize(),
                handlerRequest.getVolumesToUpdate(),
                STACK_ID
        );
    }

    @Test
    public void testResizeDisksException() throws CloudbreakOrchestratorFailedException {
        doThrow(new CloudbreakOrchestratorFailedException("TEST")).when(hostOrchestrator).resizeDisksOnNodes(anyList(), anySet(), anySet(), any());
        Selectable response = underTest.doAccept(new HandlerEvent<>(new Event<>(handlerRequest)));
        assertEquals(DiskResizeEvent.FAILURE_EVENT.event(), response.getSelector());
        assertEquals("TEST", response.getException().getMessage());
        assertEquals(CloudbreakOrchestratorFailedException.class, response.getException().getClass());
        verify(hostOrchestrator, times(1)).resizeDisksOnNodes(anyList(), anySet(), anySet(), any());
    }

}
