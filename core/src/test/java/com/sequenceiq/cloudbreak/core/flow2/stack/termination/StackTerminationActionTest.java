package com.sequenceiq.cloudbreak.core.flow2.stack.termination;

import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

import java.util.List;
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

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.resource.TerminateStackRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.TerminationType;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.consumption.AttachedVolumeConsumptionCollectionUnschedulingSuccess;
import com.sequenceiq.flow.core.FlowConstants;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowRegister;
import com.sequenceiq.flow.reactor.ErrorHandlerAwareReactorEventFactory;

import io.opentracing.SpanContext;
import reactor.bus.Event;
import reactor.bus.EventBus;

@ExtendWith(MockitoExtension.class)
class StackTerminationActionTest {

    private static final Long STACK_ID = 123L;

    private static final String FLOW_ID = "flowId";

    private static final String FLOW_TRIGGER_USER_CRN = "flowTriggerUserCrn";

    private static final String FLOW_CHAIN_ID = "flowChainId";

    @InjectMocks
    private StackTerminationAction underTest;

    @Mock
    private FlowRegister runningFlows;

    @Mock
    private ErrorHandlerAwareReactorEventFactory reactorEventFactory;

    @Mock
    private EventBus eventBus;

    @Mock
    private SpanContext spanContext;

    private FlowParameters flowParameters;

    @Mock
    private Event<?> event;

    @Captor
    private ArgumentCaptor<Map<String, Object>> headersCaptor;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(underTest, "runningFlows", runningFlows);
        ReflectionTestUtils.setField(underTest, "reactorEventFactory", reactorEventFactory);
        ReflectionTestUtils.setField(underTest, "eventBus", eventBus);

        flowParameters = new FlowParameters(FLOW_ID, FLOW_TRIGGER_USER_CRN, spanContext);

        lenient().when(runningFlows.getFlowChainId(FLOW_ID)).thenReturn(FLOW_CHAIN_ID);
        lenient().when(reactorEventFactory.createEvent(anyMap(), any())).thenReturn((Event<Object>) event);
    }

    @Test
    void doExecuteTest() {
        ArgumentCaptor<TerminateStackRequest> terminateStackRequestCaptor = ArgumentCaptor.forClass(TerminateStackRequest.class);

        underTest.doExecute(stackTerminationContext(), new AttachedVolumeConsumptionCollectionUnschedulingSuccess(STACK_ID), Map.of());

        verifyEvent(terminateStackRequestCaptor, "TERMINATESTACKREQUEST");

        TerminateStackRequest terminateStackRequest = terminateStackRequestCaptor.getValue();
        assertThat(terminateStackRequest).isNotNull();
        verifyTerminateStackRequest(terminateStackRequest);
    }

    private StackTerminationContext stackTerminationContext() {
        CloudContext cloudContext = CloudContext.Builder.builder()
                .withId(STACK_ID)
                .build();
        return new StackTerminationContext(flowParameters, null, cloudContext, null, null, List.of(), TerminationType.REGULAR);
    }

    private void verifyTerminateStackRequest(TerminateStackRequest terminateStackRequest) {
        assertThat(terminateStackRequest).isNotNull();
        assertThat(terminateStackRequest.getResourceId()).isEqualTo(STACK_ID);
        assertThat(terminateStackRequest.getSelector()).isEqualTo("TERMINATESTACKREQUEST");
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
                entry(FlowConstants.SPAN_CONTEXT, spanContext), entry(FlowConstants.FLOW_OPERATION_TYPE, "UNKNOWN"),
                entry(FlowConstants.FLOW_CHAIN_ID, FLOW_CHAIN_ID));
    }

    @Test
    void createRequestTest() {
        TerminateStackRequest terminateStackRequest = underTest.createRequest(stackTerminationContext());

        verifyTerminateStackRequest(terminateStackRequest);
    }

}