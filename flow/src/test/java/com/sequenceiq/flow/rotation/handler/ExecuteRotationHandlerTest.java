package com.sequenceiq.flow.rotation.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Optional;

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
import com.sequenceiq.cloudbreak.rotation.secret.SecretType;
import com.sequenceiq.cloudbreak.rotation.secret.usage.SecretRotationUsageProcessor;
import com.sequenceiq.flow.rotation.event.ExecuteRotationFailedEvent;
import com.sequenceiq.flow.rotation.event.ExecuteRotationFinishedEvent;
import com.sequenceiq.flow.rotation.event.ExecuteRotationTriggerEvent;
import com.sequenceiq.flow.rotation.service.SecretRotationService;

@ExtendWith(MockitoExtension.class)
public class ExecuteRotationHandlerTest {

    private static final SecretType SECRET_TYPE = mock(SecretType.class);

    private ArgumentCaptor<Event> argumentCaptor;

    @Mock
    private SecretRotationService secretRotationService;

    @Mock
    private SecretRotationUsageProcessor secretRotationUsageProcessor;

    @InjectMocks
    private ExecuteRotationHandler underTest;

    @BeforeEach
    public void setup() throws IllegalAccessException {
        argumentCaptor = ArgumentCaptor.forClass(Event.class);
        EventBus eventBus = mock(EventBus.class);
        doNothing().when(eventBus).notify(anyString(), argumentCaptor.capture());
        FieldUtils.writeField(underTest, "eventBus", eventBus, true);
        FieldUtils.writeField(underTest, "secretRotationUsageProcessor", Optional.of(secretRotationUsageProcessor), true);
    }

    @Test
    public void testHandler() {
        doNothing().when(secretRotationService).executeRotation(any(), any(), any());

        underTest.accept(Event.wrap(getTriggerEvent()));

        assertEquals(ExecuteRotationFinishedEvent.class, argumentCaptor.getValue().getData().getClass());
        verify(secretRotationUsageProcessor, times(1)).rotationStarted(any(), any(), any());
    }

    @Test
    public void testHandlerFailure() {
        doThrow(new CloudbreakServiceException("anything")).when(secretRotationService).executeRotation(any(), any(), any());

        underTest.accept(Event.wrap(getTriggerEvent()));

        assertEquals(ExecuteRotationFailedEvent.class, argumentCaptor.getValue().getData().getClass());
        verify(secretRotationUsageProcessor, times(1)).rotationStarted(any(), any(), any());
    }

    private static ExecuteRotationTriggerEvent getTriggerEvent() {
        return new ExecuteRotationTriggerEvent(null, null, null, SECRET_TYPE, null);
    }

}
