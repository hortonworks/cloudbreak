package com.sequenceiq.cloudbreak.core.flow2.chain;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.rollingvs.RollingVerticalScaleEvent.ROLLING_VERTICALSCALE_TRIGGER_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.generator.FlowOfflineStateGraphGenerator.FLOW_CONFIGS_PACKAGE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackVerticalScaleV4Request;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.event.RollingVerticalScaleFlowChainTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.RollingVerticalScaleTriggerEvent;
import com.sequenceiq.cloudbreak.dto.InstanceGroupDto;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.view.InstanceGroupView;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;
import com.sequenceiq.common.api.type.OrchestratorType;
import com.sequenceiq.flow.core.chain.config.FlowTriggerEventQueue;
import com.sequenceiq.flow.graph.FlowChainConfigGraphGeneratorUtil;

@ExtendWith(MockitoExtension.class)
class RollingVerticalScaleFlowEventChainFactoryTest {

    private static final long STACK_ID = 1L;

    private static final String HOST_GROUP = "master";

    private static final String INSTANCE_ID_1 = "i-instance-1";

    private static final String INSTANCE_ID_2 = "i-instance-2";

    private static final String INSTANCE_ID_3 = "i-instance-3";

    @InjectMocks
    private RollingVerticalScaleFlowEventChainFactory underTest;

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private StackDto stackDto;

    @Mock
    private InstanceGroupDto instanceGroupDto;

    @Mock
    private InstanceGroupView instanceGroupView;

    @Mock
    private InstanceMetadataView instanceMetadataView1;

    @Mock
    private InstanceMetadataView instanceMetadataView2;

    @Test
    void testInitEvent() {
        assertEquals(FlowChainTriggers.ROLLING_VERTICALSCALE_CHAIN_TRIGGER_EVENT, underTest.initEvent());
    }

    @Test
    void testCreateFlowTriggerEventQueue() {
        StackVerticalScaleV4Request request = new StackVerticalScaleV4Request();
        request.setGroup(HOST_GROUP);
        request.setOrchestratorType(OrchestratorType.ALL_AT_ONCE);
        RollingVerticalScaleFlowChainTriggerEvent event = new RollingVerticalScaleFlowChainTriggerEvent(
                FlowChainTriggers.ROLLING_VERTICALSCALE_CHAIN_TRIGGER_EVENT, STACK_ID, request);

        when(stackDtoService.getById(eq(STACK_ID))).thenReturn(stackDto);
        when(stackDto.getInstanceGroupDtos()).thenReturn(List.of(instanceGroupDto));
        when(instanceGroupDto.getInstanceGroup()).thenReturn(instanceGroupView);
        when(instanceGroupView.getGroupName()).thenReturn(HOST_GROUP);
        when(instanceGroupDto.getInstanceMetadataViews()).thenReturn(List.of(instanceMetadataView1, instanceMetadataView2));
        when(instanceMetadataView1.getInstanceId()).thenReturn(INSTANCE_ID_1);
        when(instanceMetadataView2.getInstanceId()).thenReturn(INSTANCE_ID_2);

        FlowTriggerEventQueue flowTriggerEventQueue = underTest.createFlowTriggerEventQueue(event);

        Queue<Selectable> queue = flowTriggerEventQueue.getQueue();
        Queue<Selectable> restrainedQueueData = new ConcurrentLinkedQueue<>(queue);
        assertEquals(1, queue.size());
        Selectable selectable = queue.poll();
        assertInstanceOf(RollingVerticalScaleTriggerEvent.class, selectable);
        RollingVerticalScaleTriggerEvent triggerEvent = (RollingVerticalScaleTriggerEvent) selectable;
        assertEquals(ROLLING_VERTICALSCALE_TRIGGER_EVENT.event(), triggerEvent.selector());
        assertEquals(STACK_ID, triggerEvent.getResourceId());
        assertEquals(List.of(INSTANCE_ID_1, INSTANCE_ID_2), triggerEvent.getInstanceIds());
        assertEquals(request, triggerEvent.getStackVerticalScaleV4Request());
        flowTriggerEventQueue.getQueue().addAll(restrainedQueueData);
        FlowChainConfigGraphGeneratorUtil.generateFor(underTest, FLOW_CONFIGS_PACKAGE, flowTriggerEventQueue);
    }

    @Test
    void testCreateFlowTriggerEventQueueWithNoMatchingHostGroup() {
        StackVerticalScaleV4Request request = new StackVerticalScaleV4Request();
        request.setGroup(HOST_GROUP);
        request.setOrchestratorType(OrchestratorType.ALL_AT_ONCE);
        RollingVerticalScaleFlowChainTriggerEvent event = new RollingVerticalScaleFlowChainTriggerEvent(
                FlowChainTriggers.ROLLING_VERTICALSCALE_CHAIN_TRIGGER_EVENT, STACK_ID, request);

        when(stackDtoService.getById(eq(STACK_ID))).thenReturn(stackDto);
        when(stackDto.getInstanceGroupDtos()).thenReturn(List.of(instanceGroupDto));
        when(instanceGroupDto.getInstanceGroup()).thenReturn(instanceGroupView);
        when(instanceGroupView.getGroupName()).thenReturn("different-host-group");

        FlowTriggerEventQueue flowTriggerEventQueue = underTest.createFlowTriggerEventQueue(event);

        Queue<Selectable> queue = flowTriggerEventQueue.getQueue();
        Queue<Selectable> restrainedQueueData = new ConcurrentLinkedQueue<>(queue);
        assertEquals(1, queue.size());
        Selectable selectable = queue.poll();
        assertInstanceOf(RollingVerticalScaleTriggerEvent.class, selectable);
        RollingVerticalScaleTriggerEvent triggerEvent = (RollingVerticalScaleTriggerEvent) selectable;
        assertEquals(ROLLING_VERTICALSCALE_TRIGGER_EVENT.event(), triggerEvent.selector());
        assertEquals(STACK_ID, triggerEvent.getResourceId());
        assertEquals(List.of(), triggerEvent.getInstanceIds());
        assertEquals(request, triggerEvent.getStackVerticalScaleV4Request());
        flowTriggerEventQueue.getQueue().addAll(restrainedQueueData);
        FlowChainConfigGraphGeneratorUtil.generateFor(underTest, FLOW_CONFIGS_PACKAGE, flowTriggerEventQueue);
    }

    @Test
    void testCreateFlowTriggerEventQueueWithMultipleHostGroups() {
        StackVerticalScaleV4Request request = new StackVerticalScaleV4Request();
        request.setGroup(HOST_GROUP);
        request.setOrchestratorType(OrchestratorType.ALL_AT_ONCE);
        RollingVerticalScaleFlowChainTriggerEvent event = new RollingVerticalScaleFlowChainTriggerEvent(
                FlowChainTriggers.ROLLING_VERTICALSCALE_CHAIN_TRIGGER_EVENT, STACK_ID, request);

        InstanceGroupDto otherInstanceGroupDto = mock(InstanceGroupDto.class);
        InstanceGroupView otherInstanceGroupView = mock(InstanceGroupView.class);
        InstanceMetadataView otherInstanceMetadataView = mock(InstanceMetadataView.class);

        when(stackDtoService.getById(eq(STACK_ID))).thenReturn(stackDto);
        when(stackDto.getInstanceGroupDtos()).thenReturn(List.of(instanceGroupDto, otherInstanceGroupDto));
        when(instanceGroupDto.getInstanceGroup()).thenReturn(instanceGroupView);
        when(instanceGroupView.getGroupName()).thenReturn(HOST_GROUP);
        when(instanceGroupDto.getInstanceMetadataViews()).thenReturn(List.of(instanceMetadataView1, instanceMetadataView2));
        when(instanceMetadataView1.getInstanceId()).thenReturn(INSTANCE_ID_1);
        when(instanceMetadataView2.getInstanceId()).thenReturn(INSTANCE_ID_2);

        when(otherInstanceGroupDto.getInstanceGroup()).thenReturn(otherInstanceGroupView);
        when(otherInstanceGroupView.getGroupName()).thenReturn("worker");
        lenient().when(otherInstanceGroupDto.getInstanceMetadataViews()).thenReturn(List.of(otherInstanceMetadataView));
        lenient().when(otherInstanceMetadataView.getInstanceId()).thenReturn("i-worker-1");

        FlowTriggerEventQueue flowTriggerEventQueue = underTest.createFlowTriggerEventQueue(event);

        Queue<Selectable> queue = flowTriggerEventQueue.getQueue();
        Queue<Selectable> restrainedQueueData = new ConcurrentLinkedQueue<>(queue);
        assertEquals(1, queue.size());
        Selectable selectable = queue.poll();
        assertInstanceOf(RollingVerticalScaleTriggerEvent.class, selectable);
        RollingVerticalScaleTriggerEvent triggerEvent = (RollingVerticalScaleTriggerEvent) selectable;
        assertEquals(ROLLING_VERTICALSCALE_TRIGGER_EVENT.event(), triggerEvent.selector());
        assertEquals(STACK_ID, triggerEvent.getResourceId());
        assertEquals(List.of(INSTANCE_ID_1, INSTANCE_ID_2), triggerEvent.getInstanceIds());
        assertEquals(request, triggerEvent.getStackVerticalScaleV4Request());
        flowTriggerEventQueue.getQueue().addAll(restrainedQueueData);
        FlowChainConfigGraphGeneratorUtil.generateFor(underTest, FLOW_CONFIGS_PACKAGE, flowTriggerEventQueue);
    }

    @Test
    void testAllAtOnceOrchestratorCreatesSingleFlowWithAllInstances() {
        StackVerticalScaleV4Request request = new StackVerticalScaleV4Request();
        request.setGroup(HOST_GROUP);
        request.setOrchestratorType(OrchestratorType.ALL_AT_ONCE);
        RollingVerticalScaleFlowChainTriggerEvent event = new RollingVerticalScaleFlowChainTriggerEvent(
                FlowChainTriggers.ROLLING_VERTICALSCALE_CHAIN_TRIGGER_EVENT, STACK_ID, request);

        mockStackWithInstances(List.of(INSTANCE_ID_1, INSTANCE_ID_2, INSTANCE_ID_3));

        FlowTriggerEventQueue flowTriggerEventQueue = underTest.createFlowTriggerEventQueue(event);

        Queue<Selectable> queue = flowTriggerEventQueue.getQueue();
        assertEquals(1, queue.size());
        RollingVerticalScaleTriggerEvent triggerEvent = (RollingVerticalScaleTriggerEvent) queue.poll();
        assertEquals(List.of(INSTANCE_ID_1, INSTANCE_ID_2, INSTANCE_ID_3), triggerEvent.getInstanceIds());
    }

    @Test
    void testOneByOneOrchestratorCreatesSingleInstanceFlows() {
        StackVerticalScaleV4Request request = new StackVerticalScaleV4Request();
        request.setGroup(HOST_GROUP);
        request.setOrchestratorType(OrchestratorType.ONE_BY_ONE);
        RollingVerticalScaleFlowChainTriggerEvent event = new RollingVerticalScaleFlowChainTriggerEvent(
                FlowChainTriggers.ROLLING_VERTICALSCALE_CHAIN_TRIGGER_EVENT, STACK_ID, request);

        mockStackWithInstances(List.of(INSTANCE_ID_1, INSTANCE_ID_2, INSTANCE_ID_3));

        FlowTriggerEventQueue flowTriggerEventQueue = underTest.createFlowTriggerEventQueue(event);

        Queue<Selectable> queue = flowTriggerEventQueue.getQueue();
        assertEquals(3, queue.size());
        List<RollingVerticalScaleTriggerEvent> triggerEvents = queue.stream()
                .map(RollingVerticalScaleTriggerEvent.class::cast)
                .toList();
        assertEquals(List.of(List.of(INSTANCE_ID_1), List.of(INSTANCE_ID_2), List.of(INSTANCE_ID_3)),
                triggerEvents.stream().map(RollingVerticalScaleTriggerEvent::getInstanceIds).toList());
    }

    private void mockStackWithInstances(List<String> instanceIds) {
        when(stackDtoService.getById(eq(STACK_ID))).thenReturn(stackDto);
        when(stackDto.getInstanceGroupDtos()).thenReturn(List.of(instanceGroupDto));
        when(instanceGroupDto.getInstanceGroup()).thenReturn(instanceGroupView);
        when(instanceGroupView.getGroupName()).thenReturn(HOST_GROUP);
        List<InstanceMetadataView> metadataViews = instanceIds.stream()
                .map(this::mockInstanceMetadataView)
                .toList();
        when(instanceGroupDto.getInstanceMetadataViews()).thenReturn(metadataViews);
    }

    private InstanceMetadataView mockInstanceMetadataView(String instanceId) {
        InstanceMetadataView metadataView = mock(InstanceMetadataView.class);
        when(metadataView.getInstanceId()).thenReturn(instanceId);
        return metadataView;
    }
}

