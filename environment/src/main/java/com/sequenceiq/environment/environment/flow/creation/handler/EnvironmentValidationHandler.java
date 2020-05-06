package com.sequenceiq.environment.environment.flow.creation.handler;

import static com.sequenceiq.environment.environment.flow.creation.event.EnvCreationHandlerSelectors.VALIDATE_ENVIRONMENT_EVENT;
import static com.sequenceiq.environment.environment.flow.creation.event.EnvCreationStateSelectors.START_NETWORK_CREATION_EVENT;

import javax.ws.rs.WebApplicationException;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.exception.WebApplicationExceptionMessageExtractor;
import com.sequenceiq.cloudbreak.exception.BadRequestException;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
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
                            try {
                                ValidationResult validationResult = validatorService.validateTelemetryLoggingStorageLocation(environment);
                                validationResult = validationResult.merge(validatorService.validateParameters(environmentDto, environmentDto.getParameters()));
                                validationResult = validationResult.merge(validatorService.validateNetworkWithProvider(environmentDto));
                                if (validationResult.hasError()) {
                                    goToFailedState(environmentDtoEvent, validationResult.getFormattedErrors());
                                } else {
                                    goToNetworkCreationState(environmentDtoEvent, environmentDto);
                                }
                            } catch (WebApplicationException e) {
                                String responseMessage = webApplicationExceptionMessageExtractor.getErrorMessage(e);
                                goToFailedState(environmentDtoEvent, e.getMessage() + ". " + responseMessage);
                            } catch (Exception e) {
                                goToFailedState(environmentDtoEvent, e.getMessage());
                            }
                        }, () -> goToFailedState(environmentDtoEvent, String.format("Environment was not found with id '%s'.", environmentDto.getId()))
                );
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
