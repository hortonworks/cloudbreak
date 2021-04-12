package com.sequenceiq.environment.environment.flow.creation.handler;

import static com.sequenceiq.environment.environment.flow.creation.event.EnvCreationHandlerSelectors.VALIDATE_ENVIRONMENT_EVENT;
import static com.sequenceiq.environment.environment.flow.creation.event.EnvCreationStateSelectors.START_NETWORK_CREATION_EVENT;

import javax.ws.rs.WebApplicationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.CloudRegions;
import com.sequenceiq.cloudbreak.common.exception.WebApplicationExceptionMessageExtractor;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.domain.RegionWrapper;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.flow.creation.event.EnvCreationEvent;
import com.sequenceiq.environment.environment.flow.creation.event.EnvCreationFailureEvent;
import com.sequenceiq.environment.environment.service.EnvironmentService;
import com.sequenceiq.environment.environment.validation.EnvironmentFlowValidatorService;
import com.sequenceiq.flow.reactor.api.event.EventSender;
import com.sequenceiq.flow.reactor.api.handler.EventSenderAwareHandler;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class EnvironmentValidationHandler extends EventSenderAwareHandler<EnvironmentDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentValidationHandler.class);

    private final WebApplicationExceptionMessageExtractor webApplicationExceptionMessageExtractor;

    private final EnvironmentFlowValidatorService validatorService;

    private final EnvironmentService environmentService;

    private final EventBus eventBus;

    protected EnvironmentValidationHandler(
            EventSender eventSender,
            EnvironmentService environmentService,
            EnvironmentFlowValidatorService validatorService,
            WebApplicationExceptionMessageExtractor messageExtractor, EventBus eventBus) {
        super(eventSender);
        this.validatorService = validatorService;
        this.environmentService = environmentService;
        webApplicationExceptionMessageExtractor = messageExtractor;
        this.eventBus = eventBus;
    }

    @Override
    public void accept(Event<EnvironmentDto> environmentDtoEvent) {
        EnvironmentDto environmentDto = environmentDtoEvent.getData();
        environmentService.findEnvironmentById(environmentDto.getId())
                .ifPresentOrElse(environment -> {
                            LOGGER.debug("Environment validation flow step started.");
                            try {
                                validateEnvironment(environmentDtoEvent, environmentDto, environment);
                            } catch (WebApplicationException e) {
                                String responseMessage = webApplicationExceptionMessageExtractor.getErrorMessage(e);
                                goToFailedState(environmentDtoEvent, e.getMessage() + ". " + responseMessage);
                            } catch (Exception e) {
                                goToFailedState(environmentDtoEvent, e.getMessage());
                            }
                        }, () -> goToFailedState(environmentDtoEvent, String.format("Environment was not found with id '%s'.", environmentDto.getId()))
                );
    }

    private void validateEnvironment(Event<EnvironmentDto> environmentDtoEvent, EnvironmentDto environmentDto, Environment environment) {
        RegionWrapper regionWrapper = environment.getRegionWrapper();
        CloudRegions cloudRegions = environmentService.getRegionsByEnvironment(environment);
        ValidationResult.ValidationResultBuilder validationBuilder = validatorService
                .validateRegionsAndLocation(regionWrapper.getName(), regionWrapper.getRegions(), environment, cloudRegions);
        validationBuilder.merge(validatorService.validateTelemetryLoggingStorageLocation(environment));
        validationBuilder.merge(validatorService.validateTelemetryLoggingStorageConfig(environment));
        validationBuilder.merge(validatorService.validateBackupStorageLocation(environment));
        validationBuilder.merge(validatorService.validateBackupStorageConfig(environment));
        validationBuilder.merge(validatorService.validateParameters(environmentDto, environmentDto.getParameters()));
        validationBuilder.merge(validatorService.validateNetworkWithProvider(environmentDto));
        validationBuilder.merge(validatorService.validateAuthentication(environmentDto));
        ValidationResult validationResult = validationBuilder.build();
        if (validationResult.hasError()) {
            goToFailedState(environmentDtoEvent, validationResult.getFormattedErrors());
        } else {
            goToNetworkCreationState(environmentDtoEvent, environmentDto);
        }
    }

    private void goToFailedState(Event<EnvironmentDto> environmentDtoEvent, String message) {
        EnvironmentDto environmentDto = environmentDtoEvent.getData();
        EnvCreationFailureEvent failureEvent = new EnvCreationFailureEvent(
                environmentDto.getId(),
                environmentDto.getName(),
                new BadRequestException(message),
                environmentDto.getResourceCrn());

        eventBus.notify(failureEvent.selector(), new Event<>(environmentDtoEvent.getHeaders(), failureEvent));
    }

    private void goToNetworkCreationState(Event<EnvironmentDto> environmentDtoEvent, EnvironmentDto environmentDto) {
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
