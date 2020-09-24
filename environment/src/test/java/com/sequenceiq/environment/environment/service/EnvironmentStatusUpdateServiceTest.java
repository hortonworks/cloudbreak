package com.sequenceiq.environment.environment.service;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Assertions;
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

@ExtendWith(MockitoExtension.class)
public class EnvironmentStatusUpdateServiceTest {

    @Mock
    private EnvironmentService environmentService;

    @Mock
    private EventSenderService eventSenderService;

    @InjectMocks
    private EnvironmentStatusUpdateService underTest;

    @Test
    public void testUpdateEnvironmentStatusAndNotify() {
        CommonContext commonContext = new CommonContext(new FlowParameters("flowId", "userCrn", null));
        EnvironmentDto environmentDto = EnvironmentDto.builder().withId(1L).withEnvironmentStatus(EnvironmentStatus.STOP_DATAHUB_FAILED).build();
        Environment environment = new Environment();

        when(environmentService.findEnvironmentById(environmentDto.getResourceId())).thenReturn(Optional.of(environment));
        when(environmentService.save(environment)).thenReturn(environment);
        when(environmentService.getEnvironmentDto(environment)).thenReturn(environmentDto);

        EnvironmentDto actual = underTest.updateEnvironmentStatusAndNotify(commonContext, environmentDto, EnvironmentStatus.STOP_DATAHUB_FAILED,
                ResourceEvent.ENVIRONMENT_VALIDATION_FAILED, EnvStartState.ENV_START_FINISHED_STATE);

        Assertions.assertEquals(EnvironmentStatus.STOP_DATAHUB_FAILED, actual.getStatus());
        verify(eventSenderService).sendEventAndNotification(environmentDto, "userCrn", ResourceEvent.ENVIRONMENT_VALIDATION_FAILED);
    }
}
