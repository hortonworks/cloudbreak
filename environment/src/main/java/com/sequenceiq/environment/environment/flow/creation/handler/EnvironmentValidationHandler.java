package com.sequenceiq.environment.environment.flow.creation.handler;

import static com.sequenceiq.environment.environment.flow.creation.event.EnvCreationHandlerSelectors.VALIDATE_ENVIRONMENT_EVENT;
import static com.sequenceiq.environment.environment.flow.creation.event.EnvCreationStateSelectors.START_NETWORK_CREATION_EVENT;

import java.util.Set;

import javax.ws.rs.WebApplicationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.cloud.model.CloudRegions;
import com.sequenceiq.cloudbreak.cloud.model.base.ResponseStatus;
import com.sequenceiq.cloudbreak.cloud.model.objectstorage.ObjectStorageValidateResponse;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.exception.WebApplicationExceptionMessageExtractor;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.common.api.backup.request.BackupRequest;
import com.sequenceiq.common.api.telemetry.request.TelemetryRequest;
import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentCloudStorageValidationRequest;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.domain.RegionWrapper;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.dto.EnvironmentValidationDto;
import com.sequenceiq.environment.environment.flow.creation.event.EnvCreationEvent;
import com.sequenceiq.environment.environment.flow.creation.event.EnvCreationFailureEvent;
import com.sequenceiq.environment.environment.service.EnvironmentService;
import com.sequenceiq.environment.environment.service.cloudstorage.CloudStorageValidator;
import com.sequenceiq.environment.environment.v1.converter.BackupConverter;
import com.sequenceiq.environment.environment.v1.converter.TelemetryApiConverter;
import com.sequenceiq.environment.environment.validation.EnvironmentFlowValidatorService;
import com.sequenceiq.environment.events.EventSenderService;
import com.sequenceiq.environment.exception.EnvironmentServiceException;
import com.sequenceiq.flow.reactor.api.event.EventSender;
import com.sequenceiq.flow.reactor.api.handler.EventSenderAwareHandler;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class EnvironmentValidationHandler extends EventSenderAwareHandler<EnvironmentValidationDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentValidationHandler.class);

    private final WebApplicationExceptionMessageExtractor webApplicationExceptionMessageExtractor;

    private final EnvironmentFlowValidatorService validatorService;

    private final EnvironmentService environmentService;

    private final EventBus eventBus;

    private final EventSenderService eventSenderService;

    private CloudStorageValidator cloudStorageValidator;

    private TelemetryApiConverter telemetryApiConverter;

    private BackupConverter backupConverter;

    protected EnvironmentValidationHandler(
            EventSender eventSender,
            EnvironmentService environmentService,
            EnvironmentFlowValidatorService validatorService,
            WebApplicationExceptionMessageExtractor messageExtractor,
            EventBus eventBus,
            EventSenderService eventSenderService,
            CloudStorageValidator cloudStorageValidator,
            TelemetryApiConverter telemetryApiConverter,
            BackupConverter backupConverter) {
        super(eventSender);
        this.validatorService = validatorService;
        this.environmentService = environmentService;
        webApplicationExceptionMessageExtractor = messageExtractor;
        this.eventBus = eventBus;
        this.eventSenderService = eventSenderService;
        this.cloudStorageValidator = cloudStorageValidator;
        this.telemetryApiConverter = telemetryApiConverter;
        this.backupConverter = backupConverter;
    }

    @Override
    public void accept(Event<EnvironmentValidationDto> environmentDtoEvent) {
        EnvironmentValidationDto environmentValidationDto = environmentDtoEvent.getData();
        EnvironmentDto environmentDto = environmentValidationDto.getEnvironmentDto();
        environmentService.findEnvironmentById(environmentDto.getId())
                .ifPresentOrElse(environment -> {
                            LOGGER.debug("Environment validation flow step started.");
                            try {
                                validateEnvironment(environmentDtoEvent, environmentValidationDto, environment);
                                validateCloudStorage(environmentDtoEvent, environmentDto);
                                goToNetworkCreationState(environmentDtoEvent, environmentDto);
                            } catch (WebApplicationException e) {
                                String responseMessage = webApplicationExceptionMessageExtractor.getErrorMessage(e);
                                goToFailedState(environmentDtoEvent, e.getMessage() + ". " + responseMessage);
                            } catch (Exception e) {
                                goToFailedState(environmentDtoEvent, e.getMessage());
                            }
                        }, () -> goToFailedState(environmentDtoEvent, String.format("Environment was not found with id '%s'.", environmentDto.getId()))
                );
    }

    private void validateCloudStorage(Event<EnvironmentValidationDto> environmentDtoEvent, EnvironmentDto environmentDto) {
        EnvironmentCloudStorageValidationRequest cloudStorageValidationRequest = new EnvironmentCloudStorageValidationRequest();
        cloudStorageValidationRequest.setCredentialCrn(environmentDto.getCredential().getResourceCrn());
        TelemetryRequest telemetryRequest = telemetryApiConverter.convertToRequest(environmentDto.getTelemetry());
        BackupRequest backupRequest = backupConverter.convertToRequest(environmentDto.getBackup());
        cloudStorageValidationRequest.setTelemetry(telemetryRequest);
        cloudStorageValidationRequest.setBackup(backupRequest);

        ObjectStorageValidateResponse response = null;
        try {
            response = cloudStorageValidator.validateCloudStorage(environmentDto.getAccountId(), cloudStorageValidationRequest);
        } catch (Exception e) {
            String message = String.format("Error occured during object storage validation, validation skipped. Error: %s", e.getMessage());
            LOGGER.warn(message);
            eventSenderService.sendEventAndNotification(environmentDto, ThreadBasedUserCrnProvider.getUserCrn(),
                    ResourceEvent.ENVIRONMENT_VALIDATION_FAILED_AND_SKIPPED, Set.of(e.getMessage()));
        }
        if (response != null && ResponseStatus.ERROR.equals(response.getStatus())) {
            throw new EnvironmentServiceException(response.getError());
        }
    }

    private void validateEnvironment(Event<EnvironmentValidationDto> environmentDtoEvent, EnvironmentValidationDto environmentValidationDto,
            Environment environment) {
        EnvironmentDto environmentDto = environmentValidationDto.getEnvironmentDto();
        RegionWrapper regionWrapper = environment.getRegionWrapper();
        CloudRegions cloudRegions = environmentService.getRegionsByEnvironment(environment);
        ValidationResult.ValidationResultBuilder validationBuilder = validatorService
                .validateRegionsAndLocation(regionWrapper.getName(), regionWrapper.getRegions(), environment, cloudRegions);
        validationBuilder.merge(validatorService.validateTelemetryLoggingStorageLocation(environment));
        validationBuilder.merge(validatorService.validateTelemetryLoggingStorageConfig(environment));
        validationBuilder.merge(validatorService.validateBackupStorageLocation(environment));
        validationBuilder.merge(validatorService.validateBackupStorageConfig(environment));
        validationBuilder.merge(validatorService.validateParameters(environmentValidationDto, environmentDto.getParameters()));
        validationBuilder.merge(validatorService.validateNetworkWithProvider(environmentValidationDto));
        validationBuilder.merge(validatorService.validateAuthentication(environmentValidationDto));
        validationBuilder.merge(validatorService.validateAwsKeysPresent(environmentValidationDto));
        ValidationResult validationResult = validationBuilder.build();
        if (validationResult.hasError()) {
            throw new EnvironmentServiceException(validationResult.getFormattedErrors());
        }
    }

    private void goToFailedState(Event<EnvironmentValidationDto> environmentDtoEvent, String message) {
        LOGGER.warn("Environment validation failed: {}", message);
        EnvironmentDto environmentDto = environmentDtoEvent.getData().getEnvironmentDto();
        EnvCreationFailureEvent failureEvent = new EnvCreationFailureEvent(
                environmentDto.getId(),
                environmentDto.getName(),
                new BadRequestException(message),
                environmentDto.getResourceCrn());

        eventBus.notify(failureEvent.selector(), new Event<>(environmentDtoEvent.getHeaders(), failureEvent));
    }

    private void goToNetworkCreationState(Event<EnvironmentValidationDto> environmentDtoEvent, EnvironmentDto environmentDto) {
        EnvCreationEvent envCreationEvent = EnvCreationEvent.builder()
                .withResourceId(environmentDto.getResourceId())
                .withSelector(START_NETWORK_CREATION_EVENT.selector())
                .withResourceCrn(environmentDto.getResourceCrn())
                .withResourceName(environmentDto.getName())
                .build();
        eventSender().sendEvent(envCreationEvent, environmentDtoEvent.getHeaders());
    }

    @Override
    public String selector() {
        return VALIDATE_ENVIRONMENT_EVENT.selector();
    }

}
