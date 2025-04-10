package com.sequenceiq.cloudbreak.core.flow2.cluster.modifyselinux;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.modifyselinux.event.CoreModifySeLinuxStateSelectors.CORE_MODIFY_SELINUX_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.modifyselinux.event.CoreModifySeLinuxStateSelectors.FINALIZE_MODIFY_SELINUX_CORE_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.modifyselinux.event.CoreModifySeLinuxStateSelectors.FINISH_MODIFY_SELINUX_CORE_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.modifyselinux.event.CoreModifySeLinuxStateSelectors.HANDLED_FAILED_MODIFY_SELINUX_CORE_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.modifyselinux.event.CoreModifySeLinuxStateSelectors.MODIFY_SELINUX_CORE_EVENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.statemachine.action.Action;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.common.metrics.MetricService;
import com.sequenceiq.cloudbreak.core.flow2.cluster.modifyselinux.event.CoreModifySeLinuxEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.modifyselinux.event.CoreModifySeLinuxFailedEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.modifyselinux.event.CoreModifySeLinuxHandlerEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.modifyselinux.event.CoreValidateModifySeLinuxHandlerEvent;
import com.sequenceiq.cloudbreak.core.flow2.stack.CloudbreakFlowMessageService;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackContext;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.service.metrics.MetricType;
import com.sequenceiq.common.model.SeLinux;
import com.sequenceiq.flow.core.AbstractActionTestSupport;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowRegister;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.ErrorHandlerAwareReactorEventFactory;

@ExtendWith(MockitoExtension.class)
class CoreEnableSeLinuxActionsTest {

    private Map<Object, Object> variables;

    private StackContext context;

    @Mock
    private FlowRegister runningFlows;

    @Mock
    private EventBus eventBus;

    @Mock
    private ErrorHandlerAwareReactorEventFactory reactorEventFactory;

    @Mock
    private FlowParameters flowParameters;

    @Captor
    private ArgumentCaptor<String> captor;

    @Mock
    private Stack stack;

    @Mock
    private CloudStack cloudStack;

    @Mock
    private CloudContext cloudContext;

    @Mock
    private CloudCredential cloudCredential;

    @Mock
    private StackUpdater stackUpdater;

    @Mock
    private CloudbreakFlowMessageService flowMessageService;

    @Mock
    private MetricService metricService;

    @InjectMocks
    private CoreModifySeLinuxActions underTest;

    @BeforeEach
    void setUp() {
        variables = new HashMap<>();
        context = new StackContext(flowParameters, stack, cloudContext, cloudCredential, cloudStack);
    }

    @Test
    void testEnableSeLinuxValidationAction() throws Exception {
        CoreModifySeLinuxEvent event = new CoreModifySeLinuxEvent(CORE_MODIFY_SELINUX_EVENT.event(), 1L, SeLinux.ENFORCING);
        doReturn(new Event<>(new Event.Headers(new HashMap<>()), event)).when(reactorEventFactory).createEvent(any(), any());
        when(stack.getId()).thenReturn(1L);
        when(stack.getType()).thenReturn(StackType.WORKLOAD);
        AbstractCoreModifySeLinuxAction<CoreModifySeLinuxEvent> action =
                (AbstractCoreModifySeLinuxAction<CoreModifySeLinuxEvent>) underTest.enableSeLinuxValidationAction();
        initActionPrivateFields(action);
        new AbstractActionTestSupport<>(action).doExecute(context, event, variables);
        verify(stackUpdater).updateStackStatus(eq(1L), eq(DetailedStackStatus.SELINUX_MODE_UPDATE_IN_PROGRESS),
                eq("Starting to validate stack for setting SELinux to 'ENFORCING'."));
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventBus).notify(captor.capture(), eventCaptor.capture());
        String selector = EventSelectorUtil.selector(CoreValidateModifySeLinuxHandlerEvent.class);
        assertEquals(selector, captor.getValue());
        assertEquals(1L, ReflectionTestUtils.getField(eventCaptor.getValue().getData(), "stackId"));
    }

    @Test
    void testEnableSeLinuxAction() throws Exception {
        CoreModifySeLinuxEvent event = new CoreModifySeLinuxEvent(MODIFY_SELINUX_CORE_EVENT.event(), 1L, SeLinux.ENFORCING);
        doReturn(new Event<>(new Event.Headers(new HashMap<>()), event)).when(reactorEventFactory).createEvent(any(), any());
        when(stack.getType()).thenReturn(StackType.WORKLOAD);
        AbstractCoreModifySeLinuxAction<CoreModifySeLinuxEvent> action =
                (AbstractCoreModifySeLinuxAction<CoreModifySeLinuxEvent>) underTest.enableSeLinuxAction();
        initActionPrivateFields(action);
        new AbstractActionTestSupport<>(action).doExecute(context, event, variables);
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventBus).notify(captor.capture(), eventCaptor.capture());
        String selector = EventSelectorUtil.selector(CoreModifySeLinuxHandlerEvent.class);
        assertEquals(selector, captor.getValue());
        assertEquals(1L, ReflectionTestUtils.getField(eventCaptor.getValue().getData(), "stackId"));
    }

    @Test
    void testFinishedAction() throws Exception {
        CoreModifySeLinuxEvent event = new CoreModifySeLinuxEvent(FINISH_MODIFY_SELINUX_CORE_EVENT.event(), 1L, SeLinux.ENFORCING);
        doReturn(new Event<>(new Event.Headers(new HashMap<>()), event)).when(reactorEventFactory).createEvent(any(), any());
        when(stack.getId()).thenReturn(1L);
        when(stack.getDisplayName()).thenReturn("test");
        when(stack.getResourceCrn()).thenReturn("testCrn");
        when(stack.getType()).thenReturn(StackType.WORKLOAD);
        AbstractCoreModifySeLinuxAction<CoreModifySeLinuxEvent> action =
                (AbstractCoreModifySeLinuxAction<CoreModifySeLinuxEvent>) underTest.finishedAction();
        initActionPrivateFields(action);
        ReflectionTestUtils.setField(action, null, metricService, MetricService.class);
        new AbstractActionTestSupport<>(action).doExecute(context, event, variables);
        verify(stackUpdater).updateStackStatus(eq(1L), eq(DetailedStackStatus.SELINUX_MODE_UPDATE_COMPLETE),
                eq("Updated SELinux mode to ENFORCING."));
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventBus).notify(captor.capture(), eventCaptor.capture());
        verify(metricService).incrementMetricCounter(eq(MetricType.MODIFY_SELINUX_SUCCESSFUL), eq("test"), eq("testCrn"));
        String selector = FINALIZE_MODIFY_SELINUX_CORE_EVENT.event();
        assertEquals(selector, captor.getValue());
        assertEquals(1L, ReflectionTestUtils.getField(eventCaptor.getValue().getData(), "stackId"));
    }

    @Test
    void testFailedAction() throws Exception {
        when(stack.getId()).thenReturn(1L);
        when(stack.getDisplayName()).thenReturn("test");
        when(stack.getResourceCrn()).thenReturn("test-crn");
        CoreModifySeLinuxFailedEvent event = new CoreModifySeLinuxFailedEvent(1L, "test-op", new CloudbreakException("test"));
        doReturn(new Event<>(new Event.Headers(new HashMap<>()), event)).when(reactorEventFactory).createEvent(any(), any());
        AbstractCoreModifySeLinuxAction<CoreModifySeLinuxFailedEvent> action =
                (AbstractCoreModifySeLinuxAction<CoreModifySeLinuxFailedEvent>) underTest.failedAction();
        initActionPrivateFields(action);
        ReflectionTestUtils.setField(action, null, metricService, MetricService.class);
        new AbstractActionTestSupport<>(action).doExecute(context, event, variables);
        verify(stackUpdater).updateStackStatus(eq(1L), eq(DetailedStackStatus.SELINUX_MODE_UPDATE_FAILED), eq("test"));
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventBus).notify(captor.capture(), eventCaptor.capture());
        verify(metricService).incrementMetricCounter(eq(MetricType.MODIFY_SELINUX_FAILED), eq("test-crn"), eq("test"), eq("1"),
                eq("Exception: test"));
        String selector = HANDLED_FAILED_MODIFY_SELINUX_CORE_EVENT.event();
        assertEquals(selector, captor.getValue());
        assertEquals(1L, ReflectionTestUtils.getField(eventCaptor.getValue().getData(), "stackId"));
    }

    private void initActionPrivateFields(Action<?, ?> action) {
        ReflectionTestUtils.setField(action, null, runningFlows, FlowRegister.class);
        ReflectionTestUtils.setField(action, null, eventBus, EventBus.class);
        ReflectionTestUtils.setField(action, null, reactorEventFactory, ErrorHandlerAwareReactorEventFactory.class);
    }
}
