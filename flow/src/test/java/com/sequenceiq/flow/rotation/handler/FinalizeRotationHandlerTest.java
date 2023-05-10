package com.sequenceiq.flow.rotation.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.flow.rotation.event.FinalizeRotationSuccessEvent;
import com.sequenceiq.flow.rotation.event.FinalizeRotationTriggerEvent;
import com.sequenceiq.flow.rotation.event.RotationFailedEvent;
import com.sequenceiq.flow.rotation.service.SecretRotationService;

@ExtendWith(MockitoExtension.class)
public class FinalizeRotationHandlerTest {

    private ArgumentCaptor<Event> argumentCaptor;

    @Mock
    private SecretRotationService secretRotationService;

    @InjectMocks
    private FinalizeRotationHandler underTest;

    @BeforeEach
    public void setup() throws IllegalAccessException {
        argumentCaptor = ArgumentCaptor.forClass(Event.class);
        EventBus eventBus = mock(EventBus.class);
        doNothing().when(eventBus).notify(anyString(), argumentCaptor.capture());
        FieldUtils.writeField(underTest, "eventBus", eventBus, true);
    }

    @Test
    public void testHandler() {
        doNothing().when(secretRotationService).finalizeRotation(any(), any(), any());

        underTest.accept(Event.wrap(getTriggerEvent()));

        assertEquals(FinalizeRotationSuccessEvent.class, argumentCaptor.getValue().getData().getClass());
    }

    @Test
    public void testHandlerFailure() {
        doThrow(new CloudbreakServiceException("anything")).when(secretRotationService).finalizeRotation(any(), any(), any());

        underTest.accept(Event.wrap(getTriggerEvent()));

        assertEquals(RotationFailedEvent.class, argumentCaptor.getValue().getData().getClass());
    }

    private static FinalizeRotationTriggerEvent getTriggerEvent() {
        return new FinalizeRotationTriggerEvent(null, null, null, null, null);
    }

}
