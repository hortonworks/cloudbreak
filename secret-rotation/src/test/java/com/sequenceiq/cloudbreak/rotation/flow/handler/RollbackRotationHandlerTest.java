package com.sequenceiq.cloudbreak.rotation.flow.handler;

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
import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.cloudbreak.rotation.flow.event.RollbackRotationTriggerEvent;
import com.sequenceiq.cloudbreak.rotation.flow.event.RotationFailedEvent;
import com.sequenceiq.cloudbreak.rotation.service.SecretRotationService;
import com.sequenceiq.cloudbreak.rotation.usage.SecretRotationUsageProcessor;

@ExtendWith(MockitoExtension.class)
public class RollbackRotationHandlerTest {

    private static final SecretType SECRET_TYPE = mock(SecretType.class);

    private ArgumentCaptor<Event> argumentCaptor;

    @Mock
    private SecretRotationService secretRotationService;

    @Mock
    private SecretRotationUsageProcessor secretRotationUsageProcessor;

    @InjectMocks
    private RollbackRotationHandler underTest;

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
        doNothing().when(secretRotationService).rollbackRotation(any(), any(), any(), any());

        underTest.accept(Event.wrap(getTriggerEvent()));

        assertEquals(RotationFailedEvent.class, argumentCaptor.getValue().getData().getClass());
        verify(secretRotationUsageProcessor, times(1)).rollbackStarted(any(), any(), any());
        verify(secretRotationUsageProcessor, times(1)).rollbackFinished(any(), any(), any());
    }

    @Test
    public void testHandlerFailure() {
        doThrow(new CloudbreakServiceException("anything")).when(secretRotationService).rollbackRotation(any(), any(), any(), any());

        underTest.accept(Event.wrap(getTriggerEvent()));

        assertEquals(RotationFailedEvent.class, argumentCaptor.getValue().getData().getClass());
        verify(secretRotationUsageProcessor, times(1)).rollbackStarted(any(), any(), any());
        verify(secretRotationUsageProcessor, times(1)).rollbackFailed(any(), any(), any(), any());
    }

    private static RollbackRotationTriggerEvent getTriggerEvent() {
        return new RollbackRotationTriggerEvent(null, null, null, SECRET_TYPE, null, null, null);
    }

}
