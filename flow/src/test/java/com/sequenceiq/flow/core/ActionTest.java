package com.sequenceiq.flow.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.metrics.MetricService;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.flow.reactor.ErrorHandlerAwareReactorEventFactory;

import io.opentracing.SpanContext;
import io.opentracing.Tracer;

@ExtendWith(MockitoExtension.class)
public abstract class ActionTest {

    @Mock
    protected MetricService metricService;

    @Mock
    protected EventBus eventBus;

    @Mock
    protected FlowRegister runningFlows;

    @Mock
    protected ErrorHandlerAwareReactorEventFactory reactorEventFactory;

    @Mock
    protected Tracer tracer;

    @Mock
    protected FlowParameters flowParameters;

    @Captor
    private ArgumentCaptor<Event<?>> eventCaptor;

    protected void setUp(CommonContext context) {
        when(context.getFlowParameters()).thenReturn(flowParameters);
        when(flowParameters.getFlowId()).thenReturn("flow-id");
        when(flowParameters.getFlowTriggerUserCrn()).thenReturn("trigger-user-crn");
        when(flowParameters.getSpanContext()).thenReturn(mock(SpanContext.class));
        when(flowParameters.getFlowOperationType()).thenReturn("flow-operation-type");

        when(reactorEventFactory.createEvent(any(), any())).then(input -> {
            Map<String, Object> headers = input.getArgument(0);
            Object payload = input.getArgument(1);
            return new Event<>(new Event.Headers(headers), payload);
        });
    }

    protected <C, E> void verifySendEvent(C context, String selector, E event) {
        verify(eventBus).notify(eq(selector), eventCaptor.capture());
        Event<?> capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent)
                .returns(event, Event::getData);
    }

}
