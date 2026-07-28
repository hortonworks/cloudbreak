package com.sequenceiq.freeipa.flow.freeipa.rollingvscale.action;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNotNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.statemachine.action.Action;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.instance.InstancesStatusResult;
import com.sequenceiq.cloudbreak.cloud.event.instance.StartInstancesRequest;
import com.sequenceiq.cloudbreak.cloud.event.instance.StartInstancesResult;
import com.sequenceiq.cloudbreak.cloud.event.instance.StopInstancesRequest;
import com.sequenceiq.cloudbreak.cloud.event.instance.StopInstancesResult;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.common.metrics.MetricService;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.flow.core.AbstractActionTestSupport;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowRegister;
import com.sequenceiq.flow.reactor.ErrorHandlerAwareReactorEventFactory;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.freeipa.converter.cloud.InstanceMetaDataToCloudInstanceConverter;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.events.EventSenderService;
import com.sequenceiq.freeipa.flow.freeipa.downscale.DownscaleFlowEvent;
import com.sequenceiq.freeipa.flow.freeipa.downscale.event.stophealthagent.StopHealthAgentRequest;
import com.sequenceiq.freeipa.flow.freeipa.rollingvscale.event.FreeIpaRollingVerticalScaleFailureEvent;
import com.sequenceiq.freeipa.flow.freeipa.rollingvscale.event.FreeIpaRollingVerticalScaleTriggerEvent;
import com.sequenceiq.freeipa.flow.freeipa.rollingvscale.event.RollingVerticalScaleDescribeRequest;
import com.sequenceiq.freeipa.flow.freeipa.rollingvscale.event.RollingVerticalScaleDescribeResult;
import com.sequenceiq.freeipa.flow.freeipa.rollingvscale.event.RollingVerticalScaleHealthCheckRequest;
import com.sequenceiq.freeipa.flow.freeipa.rollingvscale.event.RollingVerticalScaleHealthCheckResult;
import com.sequenceiq.freeipa.flow.freeipa.rollingvscale.event.RollingVerticalScaleResizeRequest;
import com.sequenceiq.freeipa.flow.freeipa.rollingvscale.event.RollingVerticalScaleResizeResult;
import com.sequenceiq.freeipa.flow.freeipa.verticalscale.model.FreeIpaVerticalScaleParameters;
import com.sequenceiq.freeipa.flow.stack.StackContext;
import com.sequenceiq.freeipa.flow.stack.StackEvent;
import com.sequenceiq.freeipa.flow.stack.stop.StopFreeIpaServicesEvent;
import com.sequenceiq.freeipa.service.loadbalancer.FreeIpaLoadBalancerService;
import com.sequenceiq.freeipa.service.operation.OperationService;
import com.sequenceiq.freeipa.service.resource.ResourceService;
import com.sequenceiq.freeipa.service.stack.StackUpdater;
import com.sequenceiq.freeipa.service.stack.instance.InstanceMetaDataService;
import com.sequenceiq.freeipa.sync.FreeipaJobService;

@ExtendWith(MockitoExtension.class)
class FreeIpaRollingVerticalScaleActionsTest {

    private static final Long STACK_ID = 1L;

    private static final String INSTANCE_ID = "i-abc123";

    @Mock
    private StackUpdater stackUpdater;

    @Mock
    private InstanceMetaDataService instanceMetaDataService;

    @Mock
    private InstanceMetaDataToCloudInstanceConverter instanceMetaDataToCloudInstanceConverter;

    @Mock
    private ResourceService resourceService;

    @Mock
    private OperationService operationService;

    @InjectMocks
    private FreeIpaRollingVerticalScaleActions underTest;

    @Mock
    private FlowParameters flowParameters;

    @Mock
    private FlowRegister runningFlows;

    @Mock
    private EventBus eventBus;

    @Mock
    private ErrorHandlerAwareReactorEventFactory reactorEventFactory;

    @Mock
    private MetricService metricService;

    @Mock
    private EventSenderService eventSenderService;

    @Mock
    private FreeipaJobService freeipaJobService;

    @Test
    void testDescribeActionSendsDescribeRequest() throws Exception {
        Action<?, ?> action = underTest.describeAction();
        initActionPrivateFields(action);

        StackContext context = createContext();
        FreeIpaVerticalScaleParameters scaleConfig = createScaleConfig();
        FreeIpaRollingVerticalScaleTriggerEvent payload =
                new FreeIpaRollingVerticalScaleTriggerEvent(STACK_ID, INSTANCE_ID, scaleConfig);
        Map<Object, Object> variables = new HashMap<>();

        Event<Object> event = mock(Event.class);
        when(reactorEventFactory.createEvent(anyMap(), isNotNull())).thenReturn(event);

        new AbstractActionTestSupport<>(
                (AbstractFreeIpaRollingVerticalScaleAction<FreeIpaRollingVerticalScaleTriggerEvent>) action)
                .doExecute(context, payload, variables);

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(reactorEventFactory).createEvent(anyMap(), captor.capture());
        assertThat(captor.getValue()).isInstanceOf(RollingVerticalScaleDescribeRequest.class);
        RollingVerticalScaleDescribeRequest req = (RollingVerticalScaleDescribeRequest) captor.getValue();
        assertThat(req.getInstanceId()).isEqualTo(INSTANCE_ID);
        assertThat(req.getScaleConfig()).isEqualTo(scaleConfig);
        verify(stackUpdater).updateStackStatus(eq(STACK_ID), eq(DetailedStackStatus.VERTICAL_SCALE_IN_PROGRESS), any(String.class));
    }

    @Test
    void testStopHealthAgentActionWithNoLbSendsFinishedEvent() throws Exception {
        Action<?, ?> action = underTest.stopHealthAgentAction();
        initActionPrivateFields(action);
        FreeIpaLoadBalancerService loadBalancerService = mock(FreeIpaLoadBalancerService.class);
        ReflectionTestUtils.setField(action, "loadBalancerService", loadBalancerService);

        StackContext context = createContext();
        FreeIpaVerticalScaleParameters scaleConfig = createScaleConfig();
        RollingVerticalScaleDescribeResult payload = new RollingVerticalScaleDescribeResult(STACK_ID, scaleConfig);
        Map<Object, Object> variables = createVariablesWithInstanceId();

        when(loadBalancerService.findByStackId(STACK_ID)).thenReturn(Optional.empty());
        Event<Object> event = mock(Event.class);
        when(reactorEventFactory.createEvent(anyMap(), isNotNull())).thenReturn(event);

        new AbstractActionTestSupport<>(
                (AbstractFreeIpaRollingVerticalScaleAction<RollingVerticalScaleDescribeResult>) action)
                .doExecute(context, payload, variables);

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(reactorEventFactory).createEvent(anyMap(), captor.capture());
        assertThat(captor.getValue()).isInstanceOf(StackEvent.class);
        assertThat(((StackEvent) captor.getValue()).selector())
                .isEqualTo(DownscaleFlowEvent.STOP_HEALTH_AGENT_FINISHED.event());
        verify(stackUpdater).updateStackStatus(eq(STACK_ID), eq(DetailedStackStatus.VERTICAL_SCALE_IN_PROGRESS), any(String.class));
    }

    @Test
    void testStopHealthAgentActionWithLbSendsStopRequest() throws Exception {
        Action<?, ?> action = underTest.stopHealthAgentAction();
        initActionPrivateFields(action);
        FreeIpaLoadBalancerService loadBalancerService = mock(FreeIpaLoadBalancerService.class);
        ReflectionTestUtils.setField(action, "loadBalancerService", loadBalancerService);

        StackContext context = createContext();
        FreeIpaVerticalScaleParameters scaleConfig = createScaleConfig();
        RollingVerticalScaleDescribeResult payload = new RollingVerticalScaleDescribeResult(STACK_ID, scaleConfig);
        Map<Object, Object> variables = createVariablesWithInstanceId();

        com.sequenceiq.freeipa.entity.LoadBalancer lb = mock(com.sequenceiq.freeipa.entity.LoadBalancer.class);
        when(loadBalancerService.findByStackId(STACK_ID)).thenReturn(Optional.of(lb));
        InstanceMetaData instance = mock(InstanceMetaData.class);
        when(instance.getDiscoveryFQDN()).thenReturn("freeipa-0.example.com");
        when(instanceMetaDataService.getByInstanceIds(STACK_ID, List.of(INSTANCE_ID))).thenReturn(Set.of(instance));
        Event<Object> event = mock(Event.class);
        when(reactorEventFactory.createEvent(anyMap(), isNotNull())).thenReturn(event);

        new AbstractActionTestSupport<>(
                (AbstractFreeIpaRollingVerticalScaleAction<RollingVerticalScaleDescribeResult>) action)
                .doExecute(context, payload, variables);

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(reactorEventFactory).createEvent(anyMap(), captor.capture());
        assertThat(captor.getValue()).isInstanceOf(StopHealthAgentRequest.class);
        assertThat(((StopHealthAgentRequest) captor.getValue()).getFqdns())
                .containsExactly("freeipa-0.example.com");
        verify(stackUpdater).updateStackStatus(eq(STACK_ID), eq(DetailedStackStatus.VERTICAL_SCALE_IN_PROGRESS), any(String.class));
    }

    @Test
    void testStopServicesActionSetsFlowVariablesAndSendsRequest() throws Exception {
        Action<?, ?> action = underTest.stopServicesAction();
        initActionPrivateFields(action);

        StackContext context = createContext();
        StackEvent payload = new StackEvent(STACK_ID);
        Map<Object, Object> variables = createVariablesWithInstanceId();

        Event<Object> event = mock(Event.class);
        when(reactorEventFactory.createEvent(anyMap(), isNotNull())).thenReturn(event);

        new AbstractActionTestSupport<>(
                (AbstractFreeIpaRollingVerticalScaleAction<StackEvent>) action)
                .doExecute(context, payload, variables);

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(reactorEventFactory).createEvent(anyMap(), captor.capture());
        assertThat(captor.getValue()).isInstanceOf(StopFreeIpaServicesEvent.class);
        StopFreeIpaServicesEvent req = (StopFreeIpaServicesEvent) captor.getValue();
        assertThat(req.getResourceId()).isEqualTo(STACK_ID);
        assertThat(req.getInstanceIds()).containsExactly(INSTANCE_ID);
    }

    @Test
    void testStopVmActionSendsStopInstanceRequest() throws Exception {
        Action<?, ?> action = underTest.stopVmAction();
        initActionPrivateFields(action);

        StackContext context = createContext();
        StackEvent payload = new StackEvent(STACK_ID);
        Map<Object, Object> variables = createVariablesWithInstanceId();

        InstanceMetaData instance = mock(InstanceMetaData.class);
        CloudInstance cloudInstance = mock(CloudInstance.class);
        when(instanceMetaDataService.getByInstanceIds(eq(STACK_ID), eq(List.of(INSTANCE_ID))))
                .thenReturn(Set.of(instance));
        when(instanceMetaDataToCloudInstanceConverter.convert(instance)).thenReturn(cloudInstance);
        when(resourceService.getAllCloudResource(STACK_ID)).thenReturn(List.of());

        Event<Object> event = mock(Event.class);
        when(reactorEventFactory.createEvent(anyMap(), isNotNull())).thenReturn(event);

        new AbstractActionTestSupport<>(
                (AbstractFreeIpaRollingVerticalScaleAction<StackEvent>) action)
                .doExecute(context, payload, variables);

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(reactorEventFactory).createEvent(anyMap(), captor.capture());
        assertThat(captor.getValue()).isInstanceOf(StopInstancesRequest.class);
        StopInstancesRequest req = (StopInstancesRequest) captor.getValue();
        assertThat(req.getCloudInstances()).containsExactly(cloudInstance);
    }

    @Test
    void testResizeActionSendsResizeRequest() throws Exception {
        Action<?, ?> action = underTest.resizeAction();
        initActionPrivateFields(action);

        StackContext context = createContext();
        StopInstancesResult payload = new StopInstancesResult(STACK_ID, new InstancesStatusResult(null, List.of()));
        Map<Object, Object> variables = createVariablesWithInstanceId();
        FreeIpaVerticalScaleParameters vsRequest = createScaleConfig();
        variables.put("ROLLING_VERTICAL_SCALE_CONFIG", vsRequest);

        when(resourceService.getAllCloudResource(STACK_ID)).thenReturn(List.of());

        Event<Object> event = mock(Event.class);
        when(reactorEventFactory.createEvent(anyMap(), isNotNull())).thenReturn(event);

        new AbstractActionTestSupport<>(
                (AbstractFreeIpaRollingVerticalScaleAction<StopInstancesResult>) action)
                .doExecute(context, payload, variables);

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(reactorEventFactory).createEvent(anyMap(), captor.capture());
        assertThat(captor.getValue()).isInstanceOf(RollingVerticalScaleResizeRequest.class);
        RollingVerticalScaleResizeRequest req = (RollingVerticalScaleResizeRequest) captor.getValue();
        assertThat(req.getGroup()).isEqualTo("master");
    }

    @Test
    void testStartVmActionSendsStartInstanceRequest() throws Exception {
        Action<?, ?> action = underTest.startVmAction();
        initActionPrivateFields(action);

        StackContext context = createContext();
        RollingVerticalScaleResizeResult payload = new RollingVerticalScaleResizeResult(STACK_ID);
        Map<Object, Object> variables = createVariablesWithInstanceId();

        InstanceMetaData instance = mock(InstanceMetaData.class);
        CloudInstance cloudInstance = mock(CloudInstance.class);
        when(instanceMetaDataService.getByInstanceIds(eq(STACK_ID), eq(List.of(INSTANCE_ID))))
                .thenReturn(Set.of(instance));
        when(instanceMetaDataToCloudInstanceConverter.convert(instance)).thenReturn(cloudInstance);
        when(resourceService.getAllCloudResource(STACK_ID)).thenReturn(List.of());

        Event<Object> event = mock(Event.class);
        when(reactorEventFactory.createEvent(anyMap(), isNotNull())).thenReturn(event);

        new AbstractActionTestSupport<>(
                (AbstractFreeIpaRollingVerticalScaleAction<RollingVerticalScaleResizeResult>) action)
                .doExecute(context, payload, variables);

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(reactorEventFactory).createEvent(anyMap(), captor.capture());
        assertThat(captor.getValue()).isInstanceOf(StartInstancesRequest.class);
        StartInstancesRequest req = (StartInstancesRequest) captor.getValue();
        assertThat(req.getCloudInstances()).containsExactly(cloudInstance);
    }

    @Test
    void testHealthCheckActionSendsHealthCheckRequest() throws Exception {
        Action<?, ?> action = underTest.healthCheckAction();
        initActionPrivateFields(action);

        StackContext context = createContext();
        StartInstancesResult payload = new StartInstancesResult(STACK_ID, new InstancesStatusResult(null, List.of()));
        Map<Object, Object> variables = createVariablesWithInstanceId();

        Event<Object> event = mock(Event.class);
        when(reactorEventFactory.createEvent(anyMap(), isNotNull())).thenReturn(event);

        new AbstractActionTestSupport<>(
                (AbstractFreeIpaRollingVerticalScaleAction<StartInstancesResult>) action)
                .doExecute(context, payload, variables);

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(reactorEventFactory).createEvent(anyMap(), captor.capture());
        assertThat(captor.getValue()).isInstanceOf(RollingVerticalScaleHealthCheckRequest.class);
        RollingVerticalScaleHealthCheckRequest req = (RollingVerticalScaleHealthCheckRequest) captor.getValue();
        assertThat(req.getInstanceId()).isEqualTo(INSTANCE_ID);
    }

    @Test
    void testFinishedActionFinalIterationUpdatesTemplate() throws Exception {
        Action<?, ?> action = underTest.finishedAction();
        initActionPrivateFields(action);

        StackContext context = createContext();
        FreeIpaVerticalScaleParameters vsRequest = createScaleConfig();
        RollingVerticalScaleHealthCheckResult payload = new RollingVerticalScaleHealthCheckResult(STACK_ID, true);
        Map<Object, Object> variables = createVariablesWithInstanceId();
        variables.put("FINAL_CHAIN", true);
        variables.put("ROLLING_VERTICAL_SCALE_CONFIG", vsRequest);

        Event<Object> event = mock(Event.class);
        when(reactorEventFactory.createEvent(anyMap(), isNotNull())).thenReturn(event);

        new AbstractActionTestSupport<>(
                (AbstractFreeIpaRollingVerticalScaleAction<RollingVerticalScaleHealthCheckResult>) action)
                .doExecute(context, payload, variables);

        verify(stackUpdater).updateStackStatus(eq(STACK_ID), eq(DetailedStackStatus.AVAILABLE), eq("Rolling vertical scale completed successfully"));
    }

    @Test
    void testFinishedActionNonFinalIterationSkipsTemplateUpdate() throws Exception {
        Action<?, ?> action = underTest.finishedAction();
        initActionPrivateFields(action);

        StackContext context = createContext();
        FreeIpaVerticalScaleParameters vsRequest = createScaleConfig();
        RollingVerticalScaleHealthCheckResult payload = new RollingVerticalScaleHealthCheckResult(STACK_ID, true);
        Map<Object, Object> variables = createVariablesWithInstanceId();
        variables.put("FINAL_CHAIN", false);
        variables.put("ROLLING_VERTICAL_SCALE_CONFIG", vsRequest);

        Event<Object> event = mock(Event.class);
        when(reactorEventFactory.createEvent(anyMap(), isNotNull())).thenReturn(event);

        new AbstractActionTestSupport<>(
                (AbstractFreeIpaRollingVerticalScaleAction<RollingVerticalScaleHealthCheckResult>) action)
                .doExecute(context, payload, variables);

        verify(stackUpdater, org.mockito.Mockito.never()).updateStackStatus(any(Long.class), any(DetailedStackStatus.class), any(String.class));
    }

    @Test
    void testFailedActionUpdatesStackStatus() throws Exception {
        Action<?, ?> action = underTest.failedAction();
        initActionPrivateFields(action);

        StackContext context = createContext();
        Exception exception = new RuntimeException("resize failed");
        FreeIpaRollingVerticalScaleFailureEvent payload = new FreeIpaRollingVerticalScaleFailureEvent(STACK_ID, exception);
        Map<Object, Object> variables = createVariablesWithInstanceId();

        Event<Object> event = mock(Event.class);
        when(reactorEventFactory.createEvent(anyMap(), isNotNull())).thenReturn(event);

        new AbstractActionTestSupport<>(
                (AbstractFreeIpaRollingVerticalScaleAction<FreeIpaRollingVerticalScaleFailureEvent>) action)
                .doExecute(context, payload, variables);

        verify(stackUpdater).updateStackStatus(eq(STACK_ID), eq(DetailedStackStatus.VERTICAL_SCALE_FAILED), any(String.class));

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(reactorEventFactory).createEvent(anyMap(), captor.capture());
        verify(eventBus).notify(eq("ROLLING_VERTICAL_SCALE_FAIL_HANDLED_EVENT"), any());
    }

    private void initActionPrivateFields(Action<?, ?> action) {
        ReflectionTestUtils.setField(action, null, runningFlows, FlowRegister.class);
        ReflectionTestUtils.setField(action, null, eventBus, EventBus.class);
        ReflectionTestUtils.setField(action, null, reactorEventFactory, ErrorHandlerAwareReactorEventFactory.class);
        ReflectionTestUtils.setField(action, null, metricService, MetricService.class);
        ReflectionTestUtils.setField(action, null, eventSenderService, EventSenderService.class);
        ReflectionTestUtils.setField(action, "jobService", freeipaJobService);
    }

    private StackContext createContext() {
        Stack stack = mock(Stack.class);
        lenient().when(stack.getId()).thenReturn(STACK_ID);
        CloudContext cloudContext = mock(CloudContext.class);
        CloudCredential cloudCredential = mock(CloudCredential.class);
        CloudInstance cloudInstanceInGroup = mock(CloudInstance.class);
        lenient().when(cloudInstanceInGroup.getInstanceId()).thenReturn(INSTANCE_ID);
        Group group = Group.builder()
                .withName("master")
                .withType(InstanceGroupType.GATEWAY)
                .withInstances(List.of(cloudInstanceInGroup))
                .build();
        CloudStack cloudStack = CloudStack.builder()
                .groups(List.of(group))
                .build();
        return new StackContext(flowParameters, stack, cloudContext, cloudCredential, cloudStack);
    }

    private Map<Object, Object> createVariablesWithInstanceId() {
        Map<Object, Object> variables = new HashMap<>();
        variables.put("INSTANCE_IDS", List.of(INSTANCE_ID));
        return variables;
    }

    private FreeIpaVerticalScaleParameters createScaleConfig() {
        return new FreeIpaVerticalScaleParameters("master", "m5.xlarge", "m5.small", null);
    }
}
