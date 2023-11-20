package com.sequenceiq.cloudbreak.rotation.flow.subrotation.handler;

import static com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType.FINALIZE;
import static com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType.PREVALIDATE;
import static com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType.ROLLBACK;
import static com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType.ROTATE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

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
import com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType;
import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.cloudbreak.rotation.flow.subrotation.event.ExecuteSubRotationFinishedEvent;
import com.sequenceiq.cloudbreak.rotation.flow.subrotation.event.ExecuteSubRotationTriggerEvent;
import com.sequenceiq.cloudbreak.rotation.flow.subrotation.event.SubRotationFailedEvent;
import com.sequenceiq.cloudbreak.rotation.service.SecretRotationOrchestrationService;

@ExtendWith(MockitoExtension.class)
public class ExecuteSubRotationHandlerTest {

    private static final SecretType SECRET_TYPE = mock(SecretType.class);

    private static final String RESOURCE_CRN = "resourceCrn";

    private ArgumentCaptor<Event> argumentCaptor;

    @Mock
    private SecretRotationOrchestrationService secretRotationOrchestrationService;

    @InjectMocks
    private ExecuteSubRotationHandler underTest;

    @BeforeEach
    public void setup() throws IllegalAccessException {
        argumentCaptor = ArgumentCaptor.forClass(Event.class);
        EventBus eventBus = mock(EventBus.class);
        doNothing().when(eventBus).notify(anyString(), argumentCaptor.capture());
        FieldUtils.writeField(underTest, "eventBus", eventBus, true);
    }

    @Test
    public void testHandlerWhenExecutionTypeIsPreValidate() {
        underTest.accept(Event.wrap(getTriggerEvent(PREVALIDATE)));

        verify(secretRotationOrchestrationService, times(1)).preValidateIfNeeded(eq(SECRET_TYPE), eq(RESOURCE_CRN), eq(PREVALIDATE));
        assertEquals(ExecuteSubRotationFinishedEvent.class, argumentCaptor.getValue().getData().getClass());
    }

    @Test
    public void testHandlerWhenExecutionTypeIsRotate() {
        underTest.accept(Event.wrap(getTriggerEvent(ROTATE)));

        verify(secretRotationOrchestrationService, times(1)).rotateIfNeeded(eq(SECRET_TYPE), eq(RESOURCE_CRN), eq(ROTATE));
        assertEquals(ExecuteSubRotationFinishedEvent.class, argumentCaptor.getValue().getData().getClass());
    }

    @Test
    public void testHandlerWhenExecutionTypeIsRollback() {
        underTest.accept(Event.wrap(getTriggerEvent(ROLLBACK)));

        verify(secretRotationOrchestrationService, times(1)).rollbackIfNeeded(eq(SECRET_TYPE), eq(RESOURCE_CRN), eq(ROLLBACK),
                argThat(e -> e.getMessage().equals("Explicit rollback")));
        assertEquals(ExecuteSubRotationFinishedEvent.class, argumentCaptor.getValue().getData().getClass());
    }

    @Test
    public void testHandlerWhenExecutionTypeIsFinalize() {
        underTest.accept(Event.wrap(getTriggerEvent(FINALIZE)));

        verify(secretRotationOrchestrationService, times(1)).finalizeIfNeeded(eq(SECRET_TYPE), eq(RESOURCE_CRN), eq(FINALIZE));
        assertEquals(ExecuteSubRotationFinishedEvent.class, argumentCaptor.getValue().getData().getClass());
    }

    @Test
    public void testHandlerFailure() {
        doThrow(new CloudbreakServiceException("anything")).when(secretRotationOrchestrationService).rotateIfNeeded(any(), any(), any());

        underTest.accept(Event.wrap(getTriggerEvent(RotationFlowExecutionType.ROTATE)));

        assertEquals(SubRotationFailedEvent.class, argumentCaptor.getValue().getData().getClass());
    }

    private static ExecuteSubRotationTriggerEvent getTriggerEvent(RotationFlowExecutionType executionType) {
        return new ExecuteSubRotationTriggerEvent(null, 1L, RESOURCE_CRN, SECRET_TYPE, executionType);
    }
}