package com.sequenceiq.cloudbreak.structuredevent.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.statemachine.state.State;
import org.springframework.statemachine.transition.Transition;
import org.springframework.statemachine.trigger.Trigger;

import com.sequenceiq.cloudbreak.structuredevent.event.FlowDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredFlowEvent;

@ExtendWith(MockitoExtension.class)
public class CDPFlowStructuredEventHandlerTest {

    @InjectMocks
    private final CDPFlowStructuredEventHandler<String, String> underTest = new CDPFlowStructuredEventHandler<>(
            "init", "final", "flowChainType", "flowType", "flowChainId", "flowId", 0L);

    @Mock
    private CDPDefaultStructuredEventClient cdpDefaultStructuredEventClient;

    @Mock
    private CDPStructuredFlowEventFactory cdpStructuredFlowEventFactory;

    @Test
    public void testTransitionWhenExceptionNullAndTransitionValueIsNull() {
        Transition<String, String> transition = mock(Transition.class);
        CDPStructuredFlowEvent event = new CDPStructuredFlowEvent();
        when(cdpStructuredFlowEventFactory.createStructuredFlowEvent(any(), any())).thenReturn(event);

        underTest.transitionEnded(transition);

        ArgumentCaptor<FlowDetails> flowDetailsCaptor = ArgumentCaptor.forClass(FlowDetails.class);
        verify(cdpStructuredFlowEventFactory).createStructuredFlowEvent(any(), flowDetailsCaptor.capture());
        verify(cdpDefaultStructuredEventClient).sendStructuredEvent(event);

        FlowDetails flowDetails = flowDetailsCaptor.getValue();
        String unknown = "unknown";
        assertEquals(unknown, flowDetails.getFlowState());
        assertEquals(unknown, flowDetails.getNextFlowState());
        assertEquals(unknown, flowDetails.getFlowEvent());
        assertEquals(0L, flowDetails.getDuration());
    }

    @Test
    public void testTransitionWhenExceptionNotNullAndTransitionValueNotNull() throws IllegalAccessException {
        Exception exception = new Exception();
        underTest.setException(exception);

        Transition<String, String> transition = mock(Transition.class);
        Trigger<String, String> trigger = mock(Trigger.class);
        State<String, String> target = mock(State.class);
        State<String, String> source = mock(State.class);
        CDPStructuredFlowEvent event = new CDPStructuredFlowEvent();
        when(cdpStructuredFlowEventFactory.createStructuredFlowEvent(any(), any(), eq(exception))).thenReturn(event);
        when(transition.getTrigger()).thenReturn(trigger);
        when(transition.getTarget()).thenReturn(target);
        when(transition.getSource()).thenReturn(source);
        when(trigger.getEvent()).thenReturn("event");
        when(target.getId()).thenReturn("target");
        when(source.getId()).thenReturn("source");
        Object field = FieldUtils.readField(underTest, "exception", true);
        assertNotNull(field);
        underTest.transitionEnded(transition);

        ArgumentCaptor<FlowDetails> flowDetailsCaptor = ArgumentCaptor.forClass(FlowDetails.class);
        verify(cdpStructuredFlowEventFactory).createStructuredFlowEvent(any(), flowDetailsCaptor.capture(), eq(exception));
        verify(cdpDefaultStructuredEventClient).sendStructuredEvent(event);

        FlowDetails flowDetails = flowDetailsCaptor.getValue();
        assertEquals("source", flowDetails.getFlowState());
        assertEquals("target", flowDetails.getNextFlowState());
        assertEquals("event", flowDetails.getFlowEvent());

        field = FieldUtils.readField(underTest, "exception", true);
        assertNull(field);
    }
}
