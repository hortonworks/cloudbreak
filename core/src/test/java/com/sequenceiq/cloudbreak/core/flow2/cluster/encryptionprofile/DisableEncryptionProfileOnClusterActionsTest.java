package com.sequenceiq.cloudbreak.core.flow2.cluster.encryptionprofile;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.encryptionprofile.DisableEncryptionProfileOnClusterStateSelectors.DISABLE_ENCRYPTION_PROFILE_ON_CLUSTER_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.encryptionprofile.DisableEncryptionProfileOnClusterStateSelectors.FINALIZE_DISABLE_ENCRYPTION_PROFILE_ON_CLUSTER_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.encryptionprofile.DisableEncryptionProfileOnClusterStateSelectors.HANDLED_FAILED_DISABLE_ENCRYPTION_PROFILE_ON_CLUSTER_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.encryptionprofile.DisableEncryptionProfileOnClusterStateSelectors.UNSET_ENCRYPTION_PROFILE_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.encryptionprofile.DisableEncryptionProfileOnClusterStateSelectors.UNSET_ENCRYPTION_PROFILE_HANDLER_EVENT;
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
import com.sequenceiq.cloudbreak.common.metrics.MetricService;
import com.sequenceiq.cloudbreak.core.flow2.cluster.AbstractClusterAction;
import com.sequenceiq.cloudbreak.core.flow2.cluster.ClusterViewContext;
import com.sequenceiq.cloudbreak.core.flow2.cluster.encryptionprofile.event.DisableEncryptionProfileFailedEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.encryptionprofile.event.DisableEncryptionProfileOnClusterEvent;
import com.sequenceiq.cloudbreak.core.flow2.stack.AbstractStackFailureAction;
import com.sequenceiq.cloudbreak.core.flow2.stack.CloudbreakFlowMessageService;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackFailureContext;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.eventbus.ErrorHandlerAwareReactorEventFactory;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.view.ClusterView;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.flow.core.AbstractActionTestSupport;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowRegister;

@ExtendWith(MockitoExtension.class)
class DisableEncryptionProfileOnClusterActionsTest {

    private Map<Object, Object> variables;

    private ClusterViewContext context;

    private StackFailureContext failureContext;

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
    private StackView stack;

    @Mock
    private ClusterView cluster;

    @Mock
    private StackUpdater stackUpdater;

    @Mock
    private CloudbreakFlowMessageService flowMessageService;

    @Mock
    private MetricService metricService;

    @InjectMocks
    private DisableEncryptionProfileOnClusterActions underTest;

    @BeforeEach
    void setUp() {
        variables = new HashMap<>();
        context = new ClusterViewContext(flowParameters, stack, cluster);
        failureContext = new StackFailureContext(flowParameters, stack, 1L);
    }

    @Test
    void testUnsetEncryptionProfileAction() throws Exception {
        DisableEncryptionProfileOnClusterEvent event = new DisableEncryptionProfileOnClusterEvent(DISABLE_ENCRYPTION_PROFILE_ON_CLUSTER_EVENT.event(), 1L);

        doReturn(new Event<>(new Event.Headers(new HashMap<>()), event)).when(reactorEventFactory).createEvent(any(), any());
        when(stack.getId()).thenReturn(1L);

        AbstractClusterAction<DisableEncryptionProfileOnClusterEvent> action =
                (AbstractClusterAction<DisableEncryptionProfileOnClusterEvent>) underTest.unsetEncryptionProfileAction();
        initActionPrivateFields(action);

        new AbstractActionTestSupport<>(action).doExecute(context, event, variables);

        verify(stackUpdater).updateStackStatus(eq(1L), eq(DetailedStackStatus.DISABLE_ENCRYPTION_PROFILE_ON_CLUSTER_IN_PROGRESS),
                eq("Disabling encryption profile on cluster"));
        verify(flowMessageService).fireEventAndLog(eq(1L), eq(ResourceEvent.DISABLE_ENCRYPTION_PROFILE_STARTED.name()),
                eq(ResourceEvent.DISABLE_ENCRYPTION_PROFILE_STARTED));
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventBus).notify(captor.capture(), eventCaptor.capture());
        assertEquals(UNSET_ENCRYPTION_PROFILE_HANDLER_EVENT.name(), captor.getValue());
        assertEquals(1L, ((DisableEncryptionProfileOnClusterEvent) eventCaptor.getValue().getData()).getResourceId());
    }

    @Test
    void testFinishedAction() throws Exception {
        DisableEncryptionProfileOnClusterEvent event =
                new DisableEncryptionProfileOnClusterEvent(UNSET_ENCRYPTION_PROFILE_FINISHED_EVENT.event(), 1L);
        doReturn(new Event<>(new Event.Headers(new HashMap<>()), event)).when(reactorEventFactory).createEvent(any(), any());
        when(stack.getId()).thenReturn(1L);

        AbstractClusterAction<DisableEncryptionProfileOnClusterEvent> action =
                (AbstractClusterAction<DisableEncryptionProfileOnClusterEvent>) underTest.finishedAction();
        initActionPrivateFields(action);

        new AbstractActionTestSupport<>(action).doExecute(context, event, variables);

        verify(stackUpdater).updateStackStatus(eq(1L), eq(DetailedStackStatus.DISABLE_ENCRYPTION_PROFILE_ON_CLUSTER_COMPLETE),
                eq("Encryption profile disabled on cluster"));
        verify(flowMessageService).fireEventAndLog(eq(1L), eq(ResourceEvent.DISABLE_ENCRYPTION_PROFILE_FINISHED.name()),
                eq(ResourceEvent.DISABLE_ENCRYPTION_PROFILE_FINISHED));
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventBus).notify(captor.capture(), eventCaptor.capture());
        String selector = FINALIZE_DISABLE_ENCRYPTION_PROFILE_ON_CLUSTER_EVENT.event();
        assertEquals(selector, captor.getValue());
    }

    @Test
    void testFailedAction() throws Exception {
        DisableEncryptionProfileFailedEvent event = new DisableEncryptionProfileFailedEvent(1L, new CloudbreakException("Failed!"));

        AbstractStackFailureAction<DisableEncryptionProfileOnClusterState, DisableEncryptionProfileOnClusterStateSelectors> action =
                (AbstractStackFailureAction<DisableEncryptionProfileOnClusterState, DisableEncryptionProfileOnClusterStateSelectors>) underTest.failedAction();
        initActionPrivateFields(action);

        new AbstractActionTestSupport<>(action).doExecute(failureContext, event, variables);

        verify(stackUpdater).updateStackStatus(eq(1L), eq(DetailedStackStatus.DISABLE_ENCRYPTION_PROFILE_ON_CLUSTER_FAILED), eq("Failed!"));
        verify(flowMessageService).fireEventAndLog(eq(1L), eq(ResourceEvent.DISABLE_ENCRYPTION_PROFILE_FAILED.name()),
                eq(ResourceEvent.DISABLE_ENCRYPTION_PROFILE_FAILED), eq("Failed!"));
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventBus).notify(captor.capture(), eventCaptor.capture());
        String selector = HANDLED_FAILED_DISABLE_ENCRYPTION_PROFILE_ON_CLUSTER_EVENT.event();
        assertEquals(selector, captor.getValue());
    }

    @Test
    void testFailedActionWithNullExceptionDoesNotThrowNpe() throws Exception {
        DisableEncryptionProfileFailedEvent event = new DisableEncryptionProfileFailedEvent(1L, null);

        AbstractStackFailureAction<DisableEncryptionProfileOnClusterState, DisableEncryptionProfileOnClusterStateSelectors> action =
                (AbstractStackFailureAction<DisableEncryptionProfileOnClusterState, DisableEncryptionProfileOnClusterStateSelectors>) underTest.failedAction();
        initActionPrivateFields(action);

        new AbstractActionTestSupport<>(action).doExecute(failureContext, event, variables);

        verify(stackUpdater).updateStackStatus(eq(1L), eq(DetailedStackStatus.DISABLE_ENCRYPTION_PROFILE_ON_CLUSTER_FAILED), eq("Unknown error"));
        verify(flowMessageService).fireEventAndLog(eq(1L), eq(ResourceEvent.DISABLE_ENCRYPTION_PROFILE_FAILED.name()),
                eq(ResourceEvent.DISABLE_ENCRYPTION_PROFILE_FAILED), eq("Unknown error"));
    }

    @Test
    void testFailedActionWithoutExceptionMessageUsesUnknownError() throws Exception {
        DisableEncryptionProfileFailedEvent event = new DisableEncryptionProfileFailedEvent(1L, new RuntimeException());

        AbstractStackFailureAction<DisableEncryptionProfileOnClusterState, DisableEncryptionProfileOnClusterStateSelectors> action =
                (AbstractStackFailureAction<DisableEncryptionProfileOnClusterState, DisableEncryptionProfileOnClusterStateSelectors>) underTest.failedAction();
        initActionPrivateFields(action);

        new AbstractActionTestSupport<>(action).doExecute(failureContext, event, variables);

        verify(stackUpdater).updateStackStatus(eq(1L), eq(DetailedStackStatus.DISABLE_ENCRYPTION_PROFILE_ON_CLUSTER_FAILED), eq("Unknown error"));
        verify(flowMessageService).fireEventAndLog(eq(1L), eq(ResourceEvent.DISABLE_ENCRYPTION_PROFILE_FAILED.name()),
                eq(ResourceEvent.DISABLE_ENCRYPTION_PROFILE_FAILED), eq("Unknown error"));
    }

    private void initActionPrivateFields(Action<?, ?> action) {
        ReflectionTestUtils.setField(action, null, runningFlows, FlowRegister.class);
        ReflectionTestUtils.setField(action, null, eventBus, EventBus.class);
        ReflectionTestUtils.setField(action, null, reactorEventFactory, ErrorHandlerAwareReactorEventFactory.class);
    }
}
