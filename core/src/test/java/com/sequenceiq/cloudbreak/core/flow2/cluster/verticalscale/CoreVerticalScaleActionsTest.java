package com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.statemachine.action.Action;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;
import com.sequenceiq.cloudbreak.common.metrics.MetricService;
import com.sequenceiq.cloudbreak.converter.spi.ResourceToCloudResourceConverter;
import com.sequenceiq.cloudbreak.converter.spi.StackToCloudStackConverter;
import com.sequenceiq.cloudbreak.core.flow2.cluster.AbstractClusterAction;
import com.sequenceiq.cloudbreak.core.flow2.cluster.ClusterViewContext;
import com.sequenceiq.cloudbreak.core.flow2.stack.CloudbreakFlowMessageService;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.CoreVerticalScaleResult;
import com.sequenceiq.cloudbreak.service.metrics.CloudbreakMetricService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.util.StackUtil;
import com.sequenceiq.cloudbreak.view.ClusterView;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.flow.core.AbstractActionTestSupport;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowRegister;
import com.sequenceiq.flow.reactor.ErrorHandlerAwareReactorEventFactory;

@ExtendWith(MockitoExtension.class)
class CoreVerticalScaleActionsTest {

    @Mock
    private CoreVerticalScaleService coreVerticalScaleService;

    @Mock
    private CloudbreakFlowMessageService flowMessageService;

    @Mock
    private StackUtil stackUtil;

    @Mock
    private StackToCloudStackConverter cloudStackConverter;

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private ResourceToCloudResourceConverter cloudResourceConverter;

    @Mock
    private FlowParameters flowParameters;

    @Mock
    private StackView stack;

    @Mock
    private ClusterView cluster;

    @Mock
    private FlowRegister runningFlows;

    @Mock
    private EventBus eventBus;

    @Mock
    private ErrorHandlerAwareReactorEventFactory reactorEventFactory;

    @Mock
    private CloudbreakMetricService metricService;

    @InjectMocks
    private CoreVerticalScaleActions underTest;

    @Test
    void testStackVerticalScaleFinishedSendMeteringEvent() throws Exception {
        AbstractClusterAction<CoreVerticalScaleResult> action = (AbstractClusterAction<CoreVerticalScaleResult>) underTest.stackVerticalScaleFinished();
        initActionPrivateFields(action);
        when(stack.getId()).thenReturn(1L);
        ClusterViewContext clusterViewContext = new ClusterViewContext(flowParameters, stack, cluster);
        CoreVerticalScaleResult payload = new CoreVerticalScaleResult(1L, ResourceStatus.CREATED, new ArrayList<>(), null);

        new AbstractActionTestSupport<>(action).doExecute(clusterViewContext, payload, Collections.emptyMap());
        verify(coreVerticalScaleService, times(1)).finishVerticalScale(eq(1L), any(), any());
    }

    private void initActionPrivateFields(Action<?, ?> action) {
        ReflectionTestUtils.setField(action, null, runningFlows, FlowRegister.class);
        ReflectionTestUtils.setField(action, null, eventBus, EventBus.class);
        ReflectionTestUtils.setField(action, null, reactorEventFactory, ErrorHandlerAwareReactorEventFactory.class);
        ReflectionTestUtils.setField(action, null, metricService, MetricService.class);
    }
}