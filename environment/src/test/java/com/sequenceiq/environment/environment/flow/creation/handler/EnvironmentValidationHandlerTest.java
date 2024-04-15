package com.sequenceiq.environment.environment.flow.creation.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.base.ResponseStatus;
import com.sequenceiq.cloudbreak.cloud.model.objectstorage.ObjectStorageValidateResponse;
import com.sequenceiq.cloudbreak.common.exception.WebApplicationExceptionMessageExtractor;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.cloudbreak.validation.ValidationResult.ValidationResultBuilder;
import com.sequenceiq.environment.credential.domain.Credential;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.dto.EnvironmentValidationDto;
import com.sequenceiq.environment.environment.flow.creation.event.EnvCreationEvent;
import com.sequenceiq.environment.environment.flow.creation.event.EnvCreationFailureEvent;
import com.sequenceiq.environment.environment.flow.creation.event.EnvCreationStateSelectors;
import com.sequenceiq.environment.environment.service.EnvironmentService;
import com.sequenceiq.environment.environment.service.cloudstorage.CloudStorageValidator;
import com.sequenceiq.environment.environment.v1.converter.BackupConverter;
import com.sequenceiq.environment.environment.v1.converter.TelemetryApiConverter;
import com.sequenceiq.environment.environment.validation.EnvironmentFlowValidatorService;
import com.sequenceiq.environment.environment.validation.ValidationType;
import com.sequenceiq.environment.events.EventSenderService;
import com.sequenceiq.flow.reactor.api.event.EventSender;

@ExtendWith(MockitoExtension.class)
class EnvironmentValidationHandlerTest {

    private static final long ENVIRONMENT_ID = 1L;

    private static final String ENVIRONMENT_CRN = "environmentCrn";

    private static final String ENVIRONMENT_NAME = "environmentName";

    @Mock
    private EventSender eventSender;

    @Mock
    private WebApplicationExceptionMessageExtractor webApplicationExceptionMessageExtractor;

    @Mock
    private EnvironmentFlowValidatorService validatorService;

    @Mock
    private EnvironmentService environmentService;

    @Mock
    private EventBus eventBus;

    @Mock
    private EventSenderService eventSenderService;

    @Mock
    private CloudStorageValidator cloudStorageValidator;

    @Mock
    private TelemetryApiConverter telemetryApiConverter;

    @Mock
    private BackupConverter backupConverter;

    @InjectMocks
    private EnvironmentValidationHandler underTest;

    @Captor
    private ArgumentCaptor<Event<EnvCreationFailureEvent>> failureEventCaptor;

    @Test
    void selectorTest() {
        assertThat(underTest.selector()).isEqualTo("VALIDATE_ENVIRONMENT_EVENT");
    }

    @Test
    void acceptAndSendNextStateEventWhenNoValidationErrorFound() {
        EnvironmentValidationDto environmentValidationDto = createEnvironmentValidationDto();
        Environment environment = new Environment();
        when(environmentService.findEnvironmentById(anyLong())).thenReturn(Optional.of(environment));
        when(validatorService.validateRegionsAndLocation(any(), any(), eq(environment), any())).thenReturn(new ValidationResultBuilder());
        when(cloudStorageValidator.validateCloudStorage(any(), any()))
                .thenReturn(ObjectStorageValidateResponse.builder().withStatus(ResponseStatus.OK).build());

        underTest.accept(Event.wrap(environmentValidationDto));

        ArgumentCaptor<EnvCreationEvent> envCreationEventCaptor = ArgumentCaptor.forClass(EnvCreationEvent.class);
        verify(eventSender, times(1)).sendEvent(envCreationEventCaptor.capture(), any());

        EnvCreationEvent envCreationEvent = envCreationEventCaptor.getValue();
        assertThat(envCreationEvent).isNotNull();
        assertThat(envCreationEvent.getResourceCrn()).isEqualTo(ENVIRONMENT_CRN);
        assertThat(envCreationEvent.getResourceId()).isEqualTo(ENVIRONMENT_ID);
        assertThat(envCreationEvent.selector()).isEqualTo("START_NETWORK_CREATION_EVENT");
    }

    @Test
    void sendEnvCreationFailureEventWhenNoEnvironmentFound() {
        EnvironmentValidationDto environmentValidationDto = createEnvironmentValidationDto();
        when(environmentService.findEnvironmentById(anyLong())).thenReturn(Optional.empty());

        underTest.accept(Event.wrap(environmentValidationDto));

        verify(eventBus, times(1)).notify(eq(EnvCreationStateSelectors.FAILED_ENV_CREATION_EVENT.name()), failureEventCaptor.capture());
        EnvCreationFailureEvent envCreationFailureEvent = failureEventCaptor.getValue().getData();
        assertEquals("Environment was not found with id '1'.", envCreationFailureEvent.getException().getMessage());
    }

    @Test
    void sendEnvCreationFailureEventWhenValidationFailed() {
        EnvironmentValidationDto environmentValidationDto = createEnvironmentValidationDto();
        Environment environment = new Environment();
        when(environmentService.findEnvironmentById(anyLong())).thenReturn(Optional.of(environment));
        when(validatorService.validateRegionsAndLocation(any(), any(), eq(environment), any())).thenReturn(new ValidationResultBuilder());
        when(cloudStorageValidator.validateCloudStorage(any(), any()))
                .thenReturn(ObjectStorageValidateResponse.builder().withStatus(ResponseStatus.ERROR).withError("Validation failed.").build());

        underTest.accept(Event.wrap(environmentValidationDto));

        verify(eventBus, times(1)).notify(eq(EnvCreationStateSelectors.FAILED_ENV_CREATION_EVENT.name()), failureEventCaptor.capture());
        EnvCreationFailureEvent envCreationFailureEvent = failureEventCaptor.getValue().getData();
        assertEquals("Validation failed.", envCreationFailureEvent.getException().getMessage());
    }

    @Test
    void sendWarningNotificationWhenValidationHasWarnings() {
        EnvironmentValidationDto environmentValidationDto = createEnvironmentValidationDto();
        Environment environment = new Environment();
        when(environmentService.findEnvironmentById(anyLong())).thenReturn(Optional.of(environment));
        when(validatorService.validateRegionsAndLocation(any(), any(), eq(environment), any())).thenReturn(new ValidationResultBuilder().warning("warning1"));
        when(validatorService.validateNetworkWithProvider(eq(environmentValidationDto))).thenReturn(new ValidationResultBuilder().warning("warning2").build());
        when(cloudStorageValidator.validateCloudStorage(any(), any()))
                .thenReturn(ObjectStorageValidateResponse.builder().withStatus(ResponseStatus.OK).build());

        underTest.accept(Event.wrap(environmentValidationDto));

        verify(eventSenderService).sendEventAndNotification(eq(environmentValidationDto.getEnvironmentDto()),
                any(),
                eq(ResourceEvent.ENVIRONMENT_VALIDATION_WARNINGS),
                eq(Set.of("1. warning1\n" +
                        "2. warning2")));
        verify(eventBus, never()).notify(eq(EnvCreationStateSelectors.FAILED_ENV_CREATION_EVENT.name()), failureEventCaptor.capture());
    }

    private EnvironmentValidationDto createEnvironmentValidationDto() {
        EnvironmentValidationDto environmentValidationDto = new EnvironmentValidationDto();
        environmentValidationDto.setValidationType(ValidationType.ENVIRONMENT_CREATION);
        EnvironmentDto environmentDto = new EnvironmentDto();
        environmentDto.setId(ENVIRONMENT_ID);
        environmentDto.setResourceCrn(ENVIRONMENT_CRN);
        environmentDto.setName(ENVIRONMENT_NAME);
        Credential credential = new Credential();
        credential.setResourceCrn("credential");
        environmentDto.setCredential(credential);
        environmentValidationDto.setEnvironmentDto(environmentDto);
        return environmentValidationDto;
    }

}