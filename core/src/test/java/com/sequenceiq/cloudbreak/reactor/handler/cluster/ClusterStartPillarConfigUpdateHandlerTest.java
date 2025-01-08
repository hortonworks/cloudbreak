package com.sequenceiq.cloudbreak.reactor.handler.cluster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.common.collect.ArrayListMultimap;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.event.model.EventStatus;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.config.update.PillarConfigUpdateService;
import com.sequenceiq.cloudbreak.core.flow2.cluster.config.update.event.PillarConfigUpdateFailed;
import com.sequenceiq.cloudbreak.core.flow2.stack.CloudbreakFlowMessageService;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.ClusterStartPillarConfigUpdateRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.ClusterStartPillarConfigUpdateResult;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@ExtendWith(MockitoExtension.class)
class ClusterStartPillarConfigUpdateHandlerTest {

    private static final Long STACK_ID = 1L;

    @Mock
    private PillarConfigUpdateService pillarConfigUpdateService;

    @Mock
    private InstanceMetaDataService instanceMetaDataService;

    @Mock
    private CloudbreakFlowMessageService cloudbreakFlowMessageService;

    @Mock
    private EventBus eventBus;

    @InjectMocks
    private ClusterStartPillarConfigUpdateHandler underTest;

    @Mock
    private InstanceMetadataView instance1;

    @Mock
    private InstanceMetadataView instance2;

    @Test
    public void testWhenPrimaryGatewayIsNotFailedButOtherNodeFailedThenReturnsSuccess() {
        ArrayListMultimap<String, String> nodesWithErrors = ArrayListMultimap.create();
        nodesWithErrors.put("worker1", "Failed state.");
        doThrow(new RuntimeException(new CloudbreakOrchestratorFailedException("Failed nodes.", nodesWithErrors)))
                .when(pillarConfigUpdateService).doConfigUpdate(anyLong());
        ClusterStartPillarConfigUpdateRequest request = new ClusterStartPillarConfigUpdateRequest(STACK_ID);
        when(instance1.getDiscoveryFQDN()).thenReturn("master");
        when(instance2.getDiscoveryFQDN()).thenReturn("worker1");
        when(instance2.getId()).thenReturn(2L);
        when(instanceMetaDataService.getPrimaryGatewayInstanceMetadata(anyLong())).thenReturn(Optional.of(instance1));
        when(instanceMetaDataService.getAllAvailableInstanceMetadataViewsByStackId(anyLong())).thenReturn(List.of(instance1, instance2));

        Selectable selectable = underTest.doAccept(new HandlerEvent<>(new Event<>(request)));

        assertInstanceOf(ClusterStartPillarConfigUpdateResult.class, selectable);
        assertEquals(EventStatus.OK, ((ClusterStartPillarConfigUpdateResult) selectable).getStatus());
        verify(instanceMetaDataService).updateInstanceStatuses(List.of(2L), InstanceStatus.SERVICES_UNHEALTHY, "Pillar configuration update failed.");
        verify(cloudbreakFlowMessageService).fireEventAndLog(STACK_ID, "UPDATE_FAILED", ResourceEvent.CLUSTER_START_INSTANCES_FAILED,
                "worker1 - Failed state.");
    }

    @Test
    public void testWhenPrimaryGatewayIsNotFailedButOtherNodeFailedThenReturnsFailure() {
        ArrayListMultimap<String, String> nodesWithErrors = ArrayListMultimap.create();
        nodesWithErrors.put("master", "Failed state.");
        nodesWithErrors.put("worker1", "Failed state.");
        doThrow(new RuntimeException(new CloudbreakOrchestratorFailedException("Failed nodes.", nodesWithErrors)))
                .when(pillarConfigUpdateService).doConfigUpdate(anyLong());
        ClusterStartPillarConfigUpdateRequest request = new ClusterStartPillarConfigUpdateRequest(STACK_ID);
        when(instance1.getDiscoveryFQDN()).thenReturn("master");
        when(instanceMetaDataService.getPrimaryGatewayInstanceMetadata(anyLong())).thenReturn(Optional.of(instance1));

        Selectable selectable = underTest.doAccept(new HandlerEvent<>(new Event<>(request)));

        assertInstanceOf(PillarConfigUpdateFailed.class, selectable);
    }
}