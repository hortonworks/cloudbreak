package com.sequenceiq.environment.environment.service;

import static org.mockito.Mockito.times;
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
import com.sequenceiq.environment.api.v1.environment.model.response.SimpleEnvironmentResponse;
import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.flow.start.EnvStartState;
import com.sequenceiq.environment.environment.v1.converter.EnvironmentResponseConverter;
import com.sequenceiq.flow.core.CommonContext;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.notification.NotificationService;

@ExtendWith(MockitoExtension.class)
public class EnvironmentStatusUpdateServiceTest {

    @Mock
    private EnvironmentService environmentService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private EnvironmentResponseConverter environmentResponseConverter;

    @InjectMocks
    private EnvironmentStatusUpdateService underTest;

    @Test
    public void testUpdateEnvironmentStatusAndNotify() {
        CommonContext commonContext = new CommonContext(new FlowParameters("flowId", "userCrn"));
        EnvironmentDto environmentDto = EnvironmentDto.builder().withId(1L).withEnvironmentStatus(EnvironmentStatus.STOP_DATAHUB_FAILED).build();
        Environment environment = new Environment();
        SimpleEnvironmentResponse simpleEnvironmentResponse = new SimpleEnvironmentResponse();

        when(environmentService.findEnvironmentById(environmentDto.getResourceId())).thenReturn(Optional.of(environment));
        when(environmentService.save(environment)).thenReturn(environment);
        when(environmentService.getEnvironmentDto(environment)).thenReturn(environmentDto);
        when(environmentResponseConverter.dtoToSimpleResponse(environmentDto)).thenReturn(simpleEnvironmentResponse);

        EnvironmentDto actual = underTest.updateEnvironmentStatusAndNotify(commonContext, environmentDto, EnvironmentStatus.STOP_DATAHUB_FAILED,
                ResourceEvent.ENVIRONMENT_VALIDATION_FAILED, EnvStartState.ENV_START_FINISHED_STATE);

        Assertions.assertEquals(EnvironmentStatus.STOP_DATAHUB_FAILED, actual.getStatus());
        verify(notificationService, times(1)).send(ResourceEvent.ENVIRONMENT_VALIDATION_FAILED, simpleEnvironmentResponse, "userCrn");
    }
}
