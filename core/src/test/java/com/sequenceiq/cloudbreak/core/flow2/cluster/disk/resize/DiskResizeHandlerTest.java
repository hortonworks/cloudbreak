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
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.cluster.util.ResourceAttributeUtil;
import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.core.flow2.cluster.disk.resize.request.DiskResizeHandlerRequest;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.resource.ResourceService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.util.StackUtil;
import com.sequenceiq.flow.reactor.api.event.BaseFailedFlowEvent;
import com.sequenceiq.flow.reactor.api.event.BaseFlowEvent;
import com.sequenceiq.flow.reactor.api.event.EventSender;

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
    private EventSender eventSender;

    @InjectMocks
    private DiskResizeHandler underTest;

    private DiskResizeHandlerRequest handlerRequest;

    @Captor
    private ArgumentCaptor<BaseFlowEvent> captor;

    @Captor
    private ArgumentCaptor<BaseFailedFlowEvent> failedCaptor;

    @BeforeEach
    public void setUp() {
        underTest = new DiskResizeHandler(eventSender);
        MockitoAnnotations.initMocks(this);
        Stack stack = mock(Stack.class);
        ClusterApi clusterApi = mock(ClusterApi.class);
        doReturn(stack).when(stackService).getByIdWithListsInTransaction(STACK_ID);
        List<Resource> resources = List.of(mock(Resource.class));
        Set<Node> allNodes = Set.of(mock(Node.class));
        doReturn(allNodes).when(stackUtil).collectNodes(stack);
        Cluster cluster = mock(Cluster.class);
        doReturn(cluster).when(stack).getCluster();
        String selector = DISK_RESIZE_HANDLER_EVENT.selector();
        handlerRequest = new DiskResizeHandlerRequest(selector, STACK_ID, "compute");
    }

    @Test
    public void testResizeDisks() {
        ReflectionTestUtils.setField(underTest, null, eventSender, EventSender.class);
        underTest.accept(new Event<>(handlerRequest));
        verify(eventSender, times(1)).sendEvent(captor.capture(), any());
        assertEquals(DiskResizeEvent.DISK_RESIZE_FINISHED_EVENT.event(), captor.getValue().getSelector());
    }

    @Test
    public void testResizeDisksException() throws CloudbreakOrchestratorFailedException {
        doThrow(new CloudbreakOrchestratorFailedException("TEST")).when(hostOrchestrator).resizeDisksOnNodes(anyList(), anySet(), anySet(), any());
        ReflectionTestUtils.setField(underTest, null, eventSender, EventSender.class);
        underTest.accept(new Event<>(handlerRequest));
        verify(eventSender, times(1)).sendEvent(failedCaptor.capture(), any());
        assertEquals(DiskResizeEvent.FAILURE_EVENT.selector(), failedCaptor.getValue().getSelector());
        verify(hostOrchestrator, times(1)).resizeDisksOnNodes(anyList(), anySet(), anySet(), any());
    }

}
