package com.sequenceiq.cloudbreak.core.flow2.cluster.start;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
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

import com.sequenceiq.cloudbreak.core.flow2.cluster.AbstractClusterAction;
import com.sequenceiq.cloudbreak.core.flow2.cluster.ClusterViewContext;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.ClusterStartPillarConfigUpdateResult;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.ClusterStartRotateHostCertificateRequest;
import com.sequenceiq.cloudbreak.view.ClusterView;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.flow.core.AbstractActionTestSupport;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowRegister;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.ErrorHandlerAwareReactorEventFactory;

@ExtendWith(MockitoExtension.class)
class ClusterStartActionsTest {

    private static final Long STACK_ID = 42L;

    @Mock
    private FlowRegister runningFlows;

    @Mock
    private EventBus eventBus;

    @Mock
    private ErrorHandlerAwareReactorEventFactory reactorEventFactory;

    @Mock
    private FlowParameters flowParameters;

    @Mock
    private StackView stackView;

    @Mock
    private ClusterView clusterView;

    @InjectMocks
    private ClusterStartActions underTest;

    @Captor
    private ArgumentCaptor<String> selectorCaptor;

    private ClusterViewContext context;

    @BeforeEach
    void setUp() {
        when(stackView.getId()).thenReturn(STACK_ID);
        context = new ClusterViewContext(flowParameters, stackView, clusterView);
    }

    @Test
    void testRotateHostCertificateIfNeededActionSendsRequest() throws Exception {
        ClusterStartPillarConfigUpdateResult payload = new ClusterStartPillarConfigUpdateResult(
                new com.sequenceiq.cloudbreak.reactor.api.event.cluster.ClusterStartPillarConfigUpdateRequest(STACK_ID));
        AbstractClusterAction<ClusterStartPillarConfigUpdateResult> action =
                (AbstractClusterAction<ClusterStartPillarConfigUpdateResult>) underTest.rotateHostCertificateIfNeeded();
        initActionPrivateFields(action);
        when(reactorEventFactory.createEvent(any(), any())).thenReturn(new com.sequenceiq.cloudbreak.eventbus.Event<>(payload));

        new AbstractActionTestSupport<>(action).doExecute(context, payload, new HashMap<>());

        verify(eventBus).notify(selectorCaptor.capture(), any());
        assertEquals(EventSelectorUtil.selector(ClusterStartRotateHostCertificateRequest.class), selectorCaptor.getValue());
    }

    private void initActionPrivateFields(Action<?, ?> action) {
        ReflectionTestUtils.setField(action, null, runningFlows, FlowRegister.class);
        ReflectionTestUtils.setField(action, null, eventBus, EventBus.class);
        ReflectionTestUtils.setField(action, null, reactorEventFactory, ErrorHandlerAwareReactorEventFactory.class);
    }
}
