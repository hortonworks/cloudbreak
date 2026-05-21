package com.sequenceiq.cloudbreak.core.flow2.cluster.disksync;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus.AVAILABLE;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus.DISK_METADATA_SYNC_FAILED;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.disksync.DiskSyncEvent.DISK_SYNC_FAILURE_HANDLED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.disksync.DiskSyncEvent.DISK_SYNC_HANDLER_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.disksync.DiskSyncEvent.FINALIZED_EVENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
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

import com.sequenceiq.cloudbreak.core.flow2.cluster.AbstractClusterAction;
import com.sequenceiq.cloudbreak.core.flow2.cluster.ClusterViewContext;
import com.sequenceiq.cloudbreak.core.flow2.cluster.disksync.request.DiskSyncProcessFinishedEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.disksync.request.DiskSyncRequest;
import com.sequenceiq.cloudbreak.core.flow2.stack.AbstractStackFailureAction;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackFailureContext;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.cloudbreak.job.disk.DiskSyncMode;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.view.ClusterView;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.flow.core.AbstractActionTestSupport;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowRegister;
import com.sequenceiq.flow.reactor.ErrorHandlerAwareReactorEventFactory;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class DiskSyncActionsTest {

    private static final long STACK_ID = 42L;

    @Mock
    private StackUpdater stackUpdater;

    @InjectMocks
    private DiskSyncActions underTest;

    private ClusterViewContext clusterContext;

    @Mock
    private FlowParameters flowParameters;

    @Mock
    private StackView stackView;

    @Mock
    private ClusterView clusterView;

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
        clusterContext = new ClusterViewContext(flowParameters, stackView, clusterView);
        when(runningFlows.getFlowChainId(any())).thenReturn(null);
        doAnswer(invocation -> new Event<>(new Event.Headers(new HashMap<>()), invocation.getArgument(1)))
                .when(reactorEventFactory).createEvent(any(), any());
    }

    @Test
    void diskSyncInitActionSendsInitiatingEvent() throws Exception {
        DiskSyncRequest request = new DiskSyncRequest(STACK_ID, DiskSyncMode.DRY_RUN);
        AbstractClusterAction<DiskSyncRequest> action = (AbstractClusterAction<DiskSyncRequest>) underTest.diskSyncInitAction();
        initActionPrivateFields(action);
        new AbstractActionTestSupport<>(action).doExecute(clusterContext, request, new HashMap<>());
        verify(eventBus).notify(selectorCaptor.capture(), any(Event.class));
        assertEquals(DISK_SYNC_HANDLER_EVENT.event(), selectorCaptor.getValue());
    }

    @Test
    void diskSyncFinishedActionUpdatesStackAndFinalizes() throws Exception {
        DiskSyncProcessFinishedEvent payload = new DiskSyncProcessFinishedEvent(STACK_ID);
        AbstractClusterAction<DiskSyncProcessFinishedEvent> action =
                (AbstractClusterAction<DiskSyncProcessFinishedEvent>) underTest.diskSyncFinishedAction();
        initActionPrivateFields(action);
        new AbstractActionTestSupport<>(action).doExecute(clusterContext, payload, new HashMap<>());
        verify(stackUpdater).updateStackStatus(eq(STACK_ID), eq(AVAILABLE), eq("Disk metadata synchronization finished."));
        verify(eventBus).notify(selectorCaptor.capture(), any(Event.class));
        assertEquals(FINALIZED_EVENT.event(), selectorCaptor.getValue());
    }

    @Test
    void diskSyncFailedActionUpdatesStackAndSendsFailureHandled() throws Exception {
        String errorReason = "sync failed";
        StackFailureEvent payload = new StackFailureEvent(STACK_ID, new Exception(errorReason));
        StackFailureContext failureContext = new StackFailureContext(flowParameters, stackView, STACK_ID);
        AbstractStackFailureAction<DiskSyncState, DiskSyncEvent> action =
                (AbstractStackFailureAction<DiskSyncState, DiskSyncEvent>) underTest.diskSyncFailedAction();
        initActionPrivateFields(action);
        Map<Object, Object> variables = new HashMap<>();
        new AbstractActionTestSupport<>(action).doExecute(failureContext, payload, variables);
        verify(stackUpdater).updateStackStatus(eq(STACK_ID), eq(DISK_METADATA_SYNC_FAILED), eq("Disk metadata synchronization failed."));
        verify(eventBus).notify(selectorCaptor.capture(), any(Event.class));
        assertEquals(DISK_SYNC_FAILURE_HANDLED_EVENT.selector(), selectorCaptor.getValue());
    }

    private void initActionPrivateFields(Action<?, ?> action) {
        ReflectionTestUtils.setField(action, null, runningFlows, FlowRegister.class);
        ReflectionTestUtils.setField(action, null, eventBus, EventBus.class);
        ReflectionTestUtils.setField(action, null, reactorEventFactory, ErrorHandlerAwareReactorEventFactory.class);
    }
}
