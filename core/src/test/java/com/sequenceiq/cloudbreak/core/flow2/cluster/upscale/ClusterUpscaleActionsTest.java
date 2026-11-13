package com.sequenceiq.cloudbreak.core.flow2.cluster.upscale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.isNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.statemachine.action.Action;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.common.metrics.MetricService;
import com.sequenceiq.cloudbreak.common.type.ClusterManagerType;
import com.sequenceiq.cloudbreak.core.flow2.event.ClusterScaleTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.stack.AbstractStackFailureAction;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackFailureContext;
import com.sequenceiq.cloudbreak.eventbus.ErrorHandlerAwareReactorEventFactory;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.ClusterUpscaleFailedConclusionRequest;
import com.sequenceiq.cloudbreak.service.metrics.CloudbreakMetricService;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.flow.core.AbstractAction;
import com.sequenceiq.flow.core.AbstractActionTestSupport;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowRegister;

@ExtendWith(MockitoExtension.class)
public class ClusterUpscaleActionsTest {

    private static final String SELECTOR = "dontcareforthistest";

    private static final Long STACK_ID = 100L;

    @Mock
    private ClusterUpscaleFlowService clusterUpscaleFlowService;

    @InjectMocks
    private ClusterUpscaleActions underTest;

    @Mock
    private FlowParameters flowParameters;

    @Mock
    private StackView stack;

    @Mock
    private FlowRegister runningFlows;

    @Mock
    private EventBus eventBus;

    @Mock
    private ErrorHandlerAwareReactorEventFactory reactorEventFactory;

    @Mock
    private Event<Object> event;

    @Mock
    private CloudbreakMetricService metricService;

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void firstActionStoresRepairHintAndFailureActionPropagatesIt(boolean repair) throws Exception {
        Map<Object, Object> variables = new HashMap<>();

        AbstractAction<ClusterUpscaleState, ClusterUpscaleEvent, ClusterUpscaleContext, ClusterScaleTriggerEvent> firstAction =
                (AbstractAction<ClusterUpscaleState, ClusterUpscaleEvent, ClusterUpscaleContext, ClusterScaleTriggerEvent>)
                        underTest.uploadUpscaleRecipesAction();
        new AbstractActionTestSupport<>(firstAction).prepareExecution(triggerEvent(repair), variables);

        ClusterUpscaleFailedConclusionRequest request = runFailureAction(variables);

        assertThat(request.isRepair()).isEqualTo(repair);
    }

    @Test
    void failureActionSendsNonRepairRequestWhenHintMissing() throws Exception {
        ClusterUpscaleFailedConclusionRequest request = runFailureAction(new HashMap<>());

        assertThat(request.isRepair()).isFalse();
    }

    private ClusterUpscaleFailedConclusionRequest runFailureAction(Map<Object, Object> variables) throws Exception {
        AbstractStackFailureAction<ClusterUpscaleState, ClusterUpscaleEvent> failureAction =
                (AbstractStackFailureAction<ClusterUpscaleState, ClusterUpscaleEvent>) underTest.clusterUpscaleFailedAction();
        initActionPrivateFields(failureAction);
        when(reactorEventFactory.createEvent(anyMap(), isNotNull())).thenReturn(event);

        StackFailureContext context = new StackFailureContext(flowParameters, stack, STACK_ID);
        StackFailureEvent payload = new StackFailureEvent(STACK_ID, new Exception("boom"));

        new AbstractActionTestSupport<>(failureAction).doExecute(context, payload, variables);

        verify(clusterUpscaleFlowService).clusterUpscaleFailed(STACK_ID, payload.getException());
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(reactorEventFactory).createEvent(anyMap(), captor.capture());
        verify(eventBus).notify(isNotNull(String.class), isNotNull());
        assertThat(captor.getValue()).isInstanceOf(ClusterUpscaleFailedConclusionRequest.class);
        return (ClusterUpscaleFailedConclusionRequest) captor.getValue();
    }

    private ClusterScaleTriggerEvent triggerEvent(boolean repair) {
        return new ClusterScaleTriggerEvent(SELECTOR, STACK_ID, Map.of(), Map.of(), Map.of(), false, false, false, false,
                ClusterManagerType.CLOUDERA_MANAGER, false, repair);
    }

    private void initActionPrivateFields(Action<?, ?> action) {
        ReflectionTestUtils.setField(action, null, runningFlows, FlowRegister.class);
        ReflectionTestUtils.setField(action, null, eventBus, EventBus.class);
        ReflectionTestUtils.setField(action, null, reactorEventFactory, ErrorHandlerAwareReactorEventFactory.class);
        ReflectionTestUtils.setField(action, null, metricService, MetricService.class);
    }
}
