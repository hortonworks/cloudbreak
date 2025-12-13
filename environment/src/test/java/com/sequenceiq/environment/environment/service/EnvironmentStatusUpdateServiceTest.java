package com.sequenceiq.environment.environment.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.flow.start.EnvStartState;
import com.sequenceiq.environment.events.EventSenderService;
import com.sequenceiq.flow.core.CommonContext;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.reactor.api.event.BaseFailedFlowEvent;

@ExtendWith(MockitoExtension.class)
public class EnvironmentStatusUpdateServiceTest {

    @Mock
    private EnvironmentService environmentService;

    @Mock
    private EventSenderService eventSenderService;

    @InjectMocks
    private EnvironmentStatusUpdateService underTest;

    private CommonContext commonContext;

    private EnvironmentDto environmentDto;

    private Environment environment;

    @BeforeEach
    void setUp() {
        commonContext = new CommonContext(new FlowParameters("flowId", "userCrn"));
        environmentDto = EnvironmentDto.builder().withId(1L).withEnvironmentStatus(EnvironmentStatus.STOP_DATAHUB_FAILED).build();

        environment = new Environment();
        when(environmentService.updateEnvironmentStatus(eq(environment), any(), nullable(String.class))).thenReturn(environment);
        when(environmentService.findEnvironmentById(environmentDto.getResourceId())).thenReturn(Optional.of(environment));
        when(environmentService.getEnvironmentDto(environment)).thenReturn(environmentDto);
    }

    @Test
    public void testUpdateEnvironmentStatusAndNotify() {
        EnvironmentDto actual = underTest.updateEnvironmentStatusAndNotify(commonContext, environmentDto, EnvironmentStatus.STOP_DATAHUB_FAILED,
                ResourceEvent.ENVIRONMENT_VALIDATION_FAILED, EnvStartState.ENV_START_FINISHED_STATE);

        assertEquals(EnvironmentStatus.STOP_DATAHUB_FAILED, actual.getStatus());
        verify(eventSenderService).sendEventAndNotification(environmentDto, "userCrn", ResourceEvent.ENVIRONMENT_VALIDATION_FAILED, Set.of());
        verify(environmentService).updateEnvironmentStatus(eq(environment), eq(EnvironmentStatus.STOP_DATAHUB_FAILED), nullable(String.class));
    }

    @Test
    public void testUpdateEnvironmentStatusAndNotifyWithMessageArgs() {
        EnvironmentDto actual = underTest.updateEnvironmentStatusAndNotify(commonContext, environmentDto, EnvironmentStatus.STOP_DATAHUB_FAILED,
                ResourceEvent.ENVIRONMENT_VALIDATION_FAILED, Set.of("message"), EnvStartState.ENV_START_FINISHED_STATE);

        assertEquals(EnvironmentStatus.STOP_DATAHUB_FAILED, actual.getStatus());
        verify(eventSenderService).sendEventAndNotification(environmentDto, "userCrn", ResourceEvent.ENVIRONMENT_VALIDATION_FAILED, Set.of("message"));
        verify(environmentService).updateEnvironmentStatus(eq(environment), eq(EnvironmentStatus.STOP_DATAHUB_FAILED), nullable(String.class));
    }

    @Test
    public void updateFailedEnvironmentStatusAndNotify() {
        BaseFailedFlowEvent failedFlowEvent = mock(BaseFailedFlowEvent.class);
        String errorHappened = "error happened";
        when(failedFlowEvent.getException()).thenReturn(new RuntimeException(errorHappened));
        when(failedFlowEvent.getResourceId()).thenReturn(environmentDto.getResourceId());
        EnvironmentDto actual = underTest.updateFailedEnvironmentStatusAndNotify(commonContext, failedFlowEvent,
                EnvironmentStatus.STOP_DATAHUB_FAILED, ResourceEvent.ENVIRONMENT_VALIDATION_FAILED, Set.of("message"), EnvStartState.ENV_START_FINISHED_STATE);

        assertEquals(EnvironmentStatus.STOP_DATAHUB_FAILED, actual.getStatus());
        verify(eventSenderService).sendEventAndNotification(environmentDto, "userCrn", ResourceEvent.ENVIRONMENT_VALIDATION_FAILED, Set.of("message"));
        verify(environmentService).updateEnvironmentStatus(eq(environment), eq(EnvironmentStatus.STOP_DATAHUB_FAILED), eq(errorHappened));
    }
}
