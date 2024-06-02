package com.sequenceiq.cloudbreak.core.flow2.stack.termination;

import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.cloudbreak.reactor.api.event.recipe.CcmKeyDeregisterSuccess;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.TerminationType;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.consumption.AttachedVolumeConsumptionCollectionUnschedulingRequest;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;
import com.sequenceiq.flow.core.FlowConstants;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowRegister;
import com.sequenceiq.flow.reactor.ErrorHandlerAwareReactorEventFactory;

@ExtendWith(MockitoExtension.class)
class AttachedVolumeConsumptionCollectionUnschedulingActionTest {

    private static final Long STACK_ID = 123L;

    private static final String FLOW_ID = "flowId";

    private static final String FLOW_TRIGGER_USER_CRN = "flowTriggerUserCrn";

    private static final String FLOW_CHAIN_ID = "flowChainId";

    @Mock
    private CloudbreakEventService cloudbreakEventService;

    @InjectMocks
    private AttachedVolumeConsumptionCollectionUnschedulingAction underTest;

    @Mock
    private FlowRegister runningFlows;

    @Mock
    private ErrorHandlerAwareReactorEventFactory reactorEventFactory;

    @Mock
    private EventBus eventBus;

    private FlowParameters flowParameters;

    @Mock
    private Event<?> event;

    @Captor
    private ArgumentCaptor<Map<String, Object>> headersCaptor;

    @Mock
    private StackDtoDelegate stack;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(underTest, "runningFlows", runningFlows);
        ReflectionTestUtils.setField(underTest, "reactorEventFactory", reactorEventFactory);
        ReflectionTestUtils.setField(underTest, "eventBus", eventBus);

        flowParameters = new FlowParameters(FLOW_ID, FLOW_TRIGGER_USER_CRN);

        lenient().when(runningFlows.getFlowChainId(FLOW_ID)).thenReturn(FLOW_CHAIN_ID);
        lenient().when(reactorEventFactory.createEvent(anyMap(), any())).thenReturn((Event<Object>) event);

        lenient().when(stack.getId()).thenReturn(STACK_ID);
    }

    @Test
    void doExecuteTest() {
        ArgumentCaptor<AttachedVolumeConsumptionCollectionUnschedulingRequest> unschedulingRequestCaptor =
                ArgumentCaptor.forClass(AttachedVolumeConsumptionCollectionUnschedulingRequest.class);

        underTest.doExecute(stackTerminationContext(), new CcmKeyDeregisterSuccess(STACK_ID), Map.of());

        verifyEvent(unschedulingRequestCaptor, "ATTACHEDVOLUMECONSUMPTIONCOLLECTIONUNSCHEDULINGREQUEST");

        verifyUnschedulingRequest(unschedulingRequestCaptor.getValue());
    }

    private StackTerminationContext stackTerminationContext() {
        return new StackTerminationContext(flowParameters, stack, null, null, null, null, TerminationType.REGULAR);
    }

    private void verifyUnschedulingRequest(AttachedVolumeConsumptionCollectionUnschedulingRequest unschedulingRequest) {
        assertThat(unschedulingRequest).isNotNull();
        assertThat(unschedulingRequest.getResourceId()).isEqualTo(STACK_ID);
        assertThat(unschedulingRequest.getSelector()).isEqualTo("ATTACHEDVOLUMECONSUMPTIONCOLLECTIONUNSCHEDULINGREQUEST");
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
    void createRequestTest() {
        AttachedVolumeConsumptionCollectionUnschedulingRequest unschedulingRequest = underTest.createRequest(stackTerminationContext());

        verifyUnschedulingRequest(unschedulingRequest);
    }

}