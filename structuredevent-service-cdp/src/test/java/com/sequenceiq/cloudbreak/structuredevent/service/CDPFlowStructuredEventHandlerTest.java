package com.sequenceiq.cloudbreak.structuredevent.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.jupiter.api.Assertions;
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
            "init", "final", "flowType", "flowId", 0L);

    @Mock
    private CDPDefaultStructuredEventClient cdpDefaultStructuredEventClient;

    @Mock
    private CDPStructuredFlowEventFactory cdpStructuredFlowEventFactory;

    @Test
    public void testTransitionWhenExceptionNullAndTransitionValueIsNull() {
        Transition<String, String> transition = mock(Transition.class);
        CDPStructuredFlowEvent event = new CDPStructuredFlowEvent();
        when(cdpStructuredFlowEventFactory.createStructuredFlowEvent(any(), any(), any())).thenReturn(event);

        underTest.transition(transition);

        ArgumentCaptor<FlowDetails> flowDetailsCaptor = ArgumentCaptor.forClass(FlowDetails.class);
        ArgumentCaptor<Boolean> detailedCaptor = ArgumentCaptor.forClass(Boolean.class);
        verify(cdpStructuredFlowEventFactory).createStructuredFlowEvent(any(), flowDetailsCaptor.capture(), detailedCaptor.capture());
        verify(cdpDefaultStructuredEventClient).sendStructuredEvent(event);

        FlowDetails flowDetails = flowDetailsCaptor.getValue();
        String unknown = "unknown";
        Assertions.assertEquals(unknown, flowDetails.getFlowState());
        Assertions.assertEquals(unknown, flowDetails.getNextFlowState());
        Assertions.assertEquals(unknown, flowDetails.getFlowEvent());
        Assertions.assertEquals(0L, flowDetails.getDuration());

        Boolean detailed = detailedCaptor.getValue();
        Assertions.assertFalse(detailed);
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
        when(cdpStructuredFlowEventFactory.createStructuredFlowEvent(any(), any(), any(), eq(exception))).thenReturn(event);
        when(transition.getTrigger()).thenReturn(trigger);
        when(transition.getTarget()).thenReturn(target);
        when(transition.getSource()).thenReturn(source);
        when(trigger.getEvent()).thenReturn("event");
        when(target.getId()).thenReturn("target");
        when(source.getId()).thenReturn("source");
        Object field = FieldUtils.readField(underTest, "exception", true);
        Assertions.assertNotNull(field);
        underTest.transition(transition);

        ArgumentCaptor<FlowDetails> flowDetailsCaptor = ArgumentCaptor.forClass(FlowDetails.class);
        ArgumentCaptor<Boolean> detailedCaptor = ArgumentCaptor.forClass(Boolean.class);
        verify(cdpStructuredFlowEventFactory).createStructuredFlowEvent(any(), flowDetailsCaptor.capture(), detailedCaptor.capture(), eq(exception));
        verify(cdpDefaultStructuredEventClient).sendStructuredEvent(event);

        FlowDetails flowDetails = flowDetailsCaptor.getValue();
        Assertions.assertEquals("source", flowDetails.getFlowState());
        Assertions.assertEquals("target", flowDetails.getNextFlowState());
        Assertions.assertEquals("event", flowDetails.getFlowEvent());

        Boolean detailed = detailedCaptor.getValue();
        Assertions.assertTrue(detailed);
        field = FieldUtils.readField(underTest, "exception", true);
        Assertions.assertNull(field);
    }
}
