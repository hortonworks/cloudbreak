package com.sequenceiq.cloudbreak.core.flow2.stack.provision.action;

import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

import java.util.Map;
import java.util.function.Supplier;

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

import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.converter.spi.InstanceMetaDataToCloudInstanceConverter;
import com.sequenceiq.cloudbreak.converter.spi.ResourceToCloudResourceConverter;
import com.sequenceiq.cloudbreak.converter.spi.StackToCloudStackConverter;
import com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationEvent;
import com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationState;
import com.sequenceiq.cloudbreak.core.flow2.stack.provision.service.StackCreationService;
import com.sequenceiq.cloudbreak.core.flow2.stack.start.StackCreationContext;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.consumption.AttachedVolumeConsumptionCollectionSchedulingSuccess;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentClientService;
import com.sequenceiq.cloudbreak.service.image.ImageService;
import com.sequenceiq.cloudbreak.service.metrics.CloudbreakMetricService;
import com.sequenceiq.cloudbreak.service.metrics.MetricType;
import com.sequenceiq.cloudbreak.service.multiaz.DataLakeAwareInstanceMetadataAvailabilityZoneCalculator;
import com.sequenceiq.cloudbreak.service.resource.ResourceService;
import com.sequenceiq.cloudbreak.service.stack.LoadBalancerPersistenceService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.flow.core.AbstractActionTestSupport;
import com.sequenceiq.flow.core.FlowConstants;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowRegister;
import com.sequenceiq.flow.reactor.ErrorHandlerAwareReactorEventFactory;

@ExtendWith(MockitoExtension.class)
class StackCreationActionsTest {

    private static final Long STACK_ID = 123L;

    private static final String FLOW_ID = "flowId";

    private static final String FLOW_TRIGGER_USER_CRN = "flowTriggerUserCrn";

    private static final String FLOW_CHAIN_ID = "flowChainId";

    @Mock
    private ImageService imageService;

    @Mock
    private StackToCloudStackConverter cloudStackConverter;

    @Mock
    private StackCreationService stackCreationService;

    @Mock
    private ResourceToCloudResourceConverter cloudResourceConverter;

    @Mock
    private InstanceMetaDataToCloudInstanceConverter metadataConverter;

    @Mock
    private LoadBalancerPersistenceService loadBalancerPersistenceService;

    @Mock
    private EnvironmentClientService environmentClientService;

    @Mock
    private ResourceService resourceService;

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private CloudbreakEventService eventService;

    @InjectMocks
    private StackCreationActions underTest;

    @Mock
    private FlowRegister runningFlows;

    @Mock
    private ErrorHandlerAwareReactorEventFactory reactorEventFactory;

    @Mock
    private EventBus eventBus;

    @Mock
    private CloudbreakMetricService cloudbreakMetricService;

    private FlowParameters flowParameters;

    @Mock
    private Event<?> event;

    @Mock
    private DataLakeAwareInstanceMetadataAvailabilityZoneCalculator dlAwareInstanceAZCalculator;

    @Captor
    private ArgumentCaptor<Map<String, Object>> headersCaptor;

    @BeforeEach
    void setUp() {
        flowParameters = new FlowParameters(FLOW_ID, FLOW_TRIGGER_USER_CRN);

        lenient().when(runningFlows.getFlowChainId(FLOW_ID)).thenReturn(FLOW_CHAIN_ID);
        lenient().when(reactorEventFactory.createEvent(anyMap(), any())).thenReturn((Event<Object>) event);
    }

    private <P extends Payload> AbstractStackCreationAction<P> initAction(Supplier<Action<?, ?>> actionSupplier) {
        AbstractStackCreationAction<P> action = (AbstractStackCreationAction<P>) actionSupplier.get();
        ReflectionTestUtils.setField(action, "runningFlows", runningFlows);
        ReflectionTestUtils.setField(action, "reactorEventFactory", reactorEventFactory);
        ReflectionTestUtils.setField(action, "eventBus", eventBus);
        ReflectionTestUtils.setField(action, "metricService", cloudbreakMetricService);
        return action;
    }

    private StackCreationContext stackCreationContext() {
        return new StackCreationContext(flowParameters, stackView(), null, null, null);
    }

    private StackView stackView() {
        Stack stack = new Stack();
        stack.setId(STACK_ID);
        return stack;
    }

    private void verifyEvent(ArgumentCaptor<?> payloadCaptor, String selectorExpected) {
        verify(reactorEventFactory).createEvent(headersCaptor.capture(), payloadCaptor.capture());
        verify(eventBus).notify(selectorExpected, event);

        verifyHeaders();
    }

    private void verifyHeaders() {
        Map<String, Object> headers = headersCaptor.getValue();
        assertThat(headers).isNotNull();
        assertThat(headers).containsOnly(entry(FlowConstants.FLOW_ID, FLOW_ID), entry(FlowConstants.FLOW_TRIGGER_USERCRN, FLOW_TRIGGER_USER_CRN),
                entry(FlowConstants.FLOW_OPERATION_TYPE, "UNKNOWN"),
                entry(FlowConstants.FLOW_CHAIN_ID, FLOW_CHAIN_ID));
    }

    @Test
    void stackCreationFinishedActionTestDoExecute() throws Exception {
        StackCreationContext context = stackCreationContext();
        ArgumentCaptor<StackEvent> stackEventCaptor = ArgumentCaptor.forClass(StackEvent.class);
        AbstractActionTestSupport<StackCreationState, StackCreationEvent, StackCreationContext,
                AttachedVolumeConsumptionCollectionSchedulingSuccess> testSupport =
                new AbstractActionTestSupport<>(initAction(underTest::stackCreationFinishedAction));

        testSupport.doExecute(context, new AttachedVolumeConsumptionCollectionSchedulingSuccess(STACK_ID), Map.of());

        verify(stackCreationService).stackCreationFinished(STACK_ID);
        verify(cloudbreakMetricService).incrementMetricCounter(MetricType.STACK_CREATION_SUCCESSFUL, context.getStack());

        verifyEvent(stackEventCaptor, "STACK_CREATION_FINISHED");

        StackEvent stackEvent = stackEventCaptor.getValue();
        assertThat(stackEvent).isNotNull();
        verifyStackEvent(stackEvent);
    }

    private void verifyStackEvent(StackEvent stackEvent) {
        assertThat(stackEvent.getResourceId()).isEqualTo(STACK_ID);
        assertThat(stackEvent.getSelector()).isEqualTo("STACK_CREATION_FINISHED");
    }

    @Test
    void stackCreationFinishedActionTestCreateRequest() {
        AbstractActionTestSupport<StackCreationState, StackCreationEvent, StackCreationContext,
                AttachedVolumeConsumptionCollectionSchedulingSuccess> testSupport =
                new AbstractActionTestSupport<>(initAction(underTest::stackCreationFinishedAction));

        Selectable result = testSupport.createRequest(stackCreationContext());

        assertThat(result).isInstanceOf(StackEvent.class);

        verifyStackEvent((StackEvent) result);
    }

}