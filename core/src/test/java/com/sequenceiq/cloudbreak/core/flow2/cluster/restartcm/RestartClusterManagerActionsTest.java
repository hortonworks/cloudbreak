package com.sequenceiq.cloudbreak.core.flow2.cluster.restartcm;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus.CLUSTER_MANAGER_RESTART_FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.AVAILABLE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.restartcm.RestartClusterManagerFlowEvent.RESTART_CLUSTER_MANAGER_FAIL_HANDLED_EVENT;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_MANAGER_SERVER_RESTART_FAILED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;

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

import com.sequenceiq.cloudbreak.core.flow2.stack.AbstractStackFailureAction;
import com.sequenceiq.cloudbreak.core.flow2.stack.CloudbreakFlowMessageService;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackFailureContext;
import com.sequenceiq.cloudbreak.eventbus.ErrorHandlerAwareReactorEventFactory;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.flow.core.AbstractActionTestSupport;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowRegister;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class RestartClusterManagerActionsTest {

    private static final long STACK_ID = 42L;

    @Mock
    private StackUpdater stackUpdater;

    @Mock
    private CloudbreakFlowMessageService flowMessageService;

    @InjectMocks
    private RestartClusterManagerActions underTest;

    @Mock
    private FlowParameters flowParameters;

    @Mock
    private StackView stackView;

    @Mock
    private FlowRegister runningFlows;

    @Mock
    private EventBus eventBus;

    @Mock
    private ErrorHandlerAwareReactorEventFactory reactorEventFactory;

    @Captor
    private ArgumentCaptor<String> selectorCaptor;

    @BeforeEach
    void setUp() {
        when(runningFlows.getFlowChainId(any())).thenReturn(null);
        doAnswer(invocation -> new Event<>(new Event.Headers(new HashMap<>()), invocation.getArgument(1)))
                .when(reactorEventFactory).createEvent(any(), any());
    }

    @Test
    void restartClusterManagerFailedActionUpdatesStackStatusWithExceptionMessage() throws Exception {
        StackFailureEvent payload = new StackFailureEvent(STACK_ID, new Exception("Failed!"));

        executeFailedAction(payload);

        verify(stackUpdater).updateStackStatus(eq(STACK_ID), eq(CLUSTER_MANAGER_RESTART_FAILED), eq("Failed!"));
        verify(flowMessageService).fireEventAndLog(eq(STACK_ID), eq(AVAILABLE.name()), eq(CLUSTER_MANAGER_SERVER_RESTART_FAILED));
        verify(eventBus).notify(selectorCaptor.capture(), any(Event.class));
        assertEquals(RESTART_CLUSTER_MANAGER_FAIL_HANDLED_EVENT.event(), selectorCaptor.getValue());
    }

    @Test
    void restartClusterManagerFailedActionUpdatesStackStatusWithEmptyReasonWhenExceptionIsMissing() throws Exception {
        StackFailureEvent payload = new StackFailureEvent(STACK_ID, null);

        executeFailedAction(payload);

        verify(stackUpdater).updateStackStatus(eq(STACK_ID), eq(CLUSTER_MANAGER_RESTART_FAILED), eq(""));
        verify(eventBus).notify(selectorCaptor.capture(), any(Event.class));
        assertEquals(RESTART_CLUSTER_MANAGER_FAIL_HANDLED_EVENT.event(), selectorCaptor.getValue());
    }

    private void executeFailedAction(StackFailureEvent payload) throws Exception {
        StackFailureContext failureContext = new StackFailureContext(flowParameters, stackView, STACK_ID);
        AbstractStackFailureAction<RestartClusterManagerFlowState, RestartClusterManagerFlowEvent> action =
                (AbstractStackFailureAction<RestartClusterManagerFlowState, RestartClusterManagerFlowEvent>) underTest.restartClusterManagerFailedAction();
        initActionPrivateFields(action);
        new AbstractActionTestSupport<>(action).doExecute(failureContext, payload, new HashMap<>());
    }

    private void initActionPrivateFields(Action<?, ?> action) {
        ReflectionTestUtils.setField(action, null, runningFlows, FlowRegister.class);
        ReflectionTestUtils.setField(action, null, eventBus, EventBus.class);
        ReflectionTestUtils.setField(action, null, reactorEventFactory, ErrorHandlerAwareReactorEventFactory.class);
    }
}
