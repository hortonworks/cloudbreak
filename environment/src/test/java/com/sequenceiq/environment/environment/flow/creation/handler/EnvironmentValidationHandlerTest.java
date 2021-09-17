package com.sequenceiq.environment.environment.flow.creation.handler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.base.ResponseStatus;
import com.sequenceiq.cloudbreak.cloud.model.objectstorage.ObjectStorageValidateResponse;
import com.sequenceiq.cloudbreak.common.exception.WebApplicationExceptionMessageExtractor;
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

import reactor.bus.Event;
import reactor.bus.EventBus;

@ExtendWith(MockitoExtension.class)
class EnvironmentValidationHandlerTest {

    private final EventSender eventSender = mock(EventSender.class);

    private final WebApplicationExceptionMessageExtractor webApplicationExceptionMessageExtractor = mock(WebApplicationExceptionMessageExtractor.class);

    private final EnvironmentFlowValidatorService validatorService = mock(EnvironmentFlowValidatorService.class);

    private final EnvironmentService environmentService = mock(EnvironmentService.class);

    private final EventBus eventBus = mock(EventBus.class);

    private final EventSenderService eventSenderService = mock(EventSenderService.class);

    private final CloudStorageValidator cloudStorageValidator = mock(CloudStorageValidator.class);

    private final TelemetryApiConverter telemetryApiConverter = mock(TelemetryApiConverter.class);

    private final BackupConverter backupConverter = mock(BackupConverter.class);

    private final EnvironmentValidationHandler underTest = new EnvironmentValidationHandler(eventSender, environmentService, validatorService,
            webApplicationExceptionMessageExtractor, eventBus, eventSenderService, cloudStorageValidator, telemetryApiConverter, backupConverter);

    @Test
    void acceptAndSendStartNetworkCreationEventWhenNoValidationErrorFound() {
        EnvironmentValidationDto environmentValidationDto = createEnvironmentValidationDto();
        Environment environment = new Environment();
        when(environmentService.findEnvironmentById(anyLong())).thenReturn(Optional.of(environment));
        when(validatorService.validateRegionsAndLocation(any(), any(), eq(environment), any())).thenReturn(new ValidationResultBuilder());
        when(cloudStorageValidator.validateCloudStorage(any(), any()))
                .thenReturn(ObjectStorageValidateResponse.builder().withStatus(ResponseStatus.OK).build());

        underTest.accept(Event.wrap(environmentValidationDto));

        verify(eventSender, times(1)).sendEvent(any(EnvCreationEvent.class), any());
    }

    @Test
    void sendEnvCreationFailureEventWhenNoEnvironmentFound() {
        EnvironmentValidationDto environmentValidationDto = createEnvironmentValidationDto();
        when(environmentService.findEnvironmentById(anyLong())).thenReturn(Optional.empty());

        underTest.accept(Event.wrap(environmentValidationDto));

        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventBus, times(1)).notify(eq(EnvCreationStateSelectors.FAILED_ENV_CREATION_EVENT.name()), eventCaptor.capture());
        EnvCreationFailureEvent envCreationFailureEvent = (EnvCreationFailureEvent) eventCaptor.getValue().getData();
        Assertions.assertEquals("Environment was not found with id '1'.", envCreationFailureEvent.getException().getMessage());
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

        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventBus, times(1)).notify(eq(EnvCreationStateSelectors.FAILED_ENV_CREATION_EVENT.name()), eventCaptor.capture());
        EnvCreationFailureEvent envCreationFailureEvent = (EnvCreationFailureEvent) eventCaptor.getValue().getData();
        Assertions.assertEquals("Validation failed.", envCreationFailureEvent.getException().getMessage());
    }

    private EnvironmentValidationDto createEnvironmentValidationDto() {
        EnvironmentValidationDto environmentValidationDto = new EnvironmentValidationDto();
        environmentValidationDto.setValidationType(ValidationType.ENVIRONMENT_CREATION);
        EnvironmentDto environmentDto = new EnvironmentDto();
        environmentDto.setId(1L);
        Credential credential = new Credential();
        credential.setResourceCrn("credential");
        environmentDto.setCredential(credential);
        environmentValidationDto.setEnvironmentDto(environmentDto);
        return environmentValidationDto;
    }
}