package com.sequenceiq.environment.environment.flow.deletion.handler;

import static com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteHandlerSelectors.DELETE_PREREQUISITES_EVENT;
import static com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteStateSelectors.FINISH_ENV_DELETE_EVENT;

import java.util.Optional;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.Setup;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudPlatformVariant;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.exception.BadRequestException;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.dto.EnvironmentDeletionDto;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.flow.creation.event.EnvCreationEvent;
import com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteFailedEvent;
import com.sequenceiq.environment.environment.flow.deletion.handler.converter.EnvironmentDtoToPrerequisiteDeleteRequestConverter;
import com.sequenceiq.environment.environment.service.EnvironmentService;
import com.sequenceiq.flow.reactor.api.event.EventSender;
import com.sequenceiq.flow.reactor.api.handler.EventSenderAwareHandler;

import reactor.bus.Event;

@Component
public class PrerequisitesDeleteHandler extends EventSenderAwareHandler<EnvironmentDeletionDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(PrerequisitesDeleteHandler.class);

    private final EnvironmentService environmentService;

    private final EnvironmentDtoToPrerequisiteDeleteRequestConverter environmentDtoToPrerequisiteDeleteRequestConverter;

    private final CloudPlatformConnectors cloudPlatformConnectors;

    protected PrerequisitesDeleteHandler(EventSender eventSender, EnvironmentService environmentService,
            CloudPlatformConnectors cloudPlatformConnectors,
            EnvironmentDtoToPrerequisiteDeleteRequestConverter environmentDtoToPrerequisiteDeleteRequestConverter) {
        super(eventSender);
        this.environmentService = environmentService;
        this.cloudPlatformConnectors = cloudPlatformConnectors;
        this.environmentDtoToPrerequisiteDeleteRequestConverter = environmentDtoToPrerequisiteDeleteRequestConverter;
    }

    @Override
    public String selector() {
        return DELETE_PREREQUISITES_EVENT.selector();
    }

    @Override
    public void accept(Event<EnvironmentDeletionDto> environmentDtoEvent) {
        EnvironmentDeletionDto environmentDeletionDto = environmentDtoEvent.getData();
        EnvironmentDto environmentDto = environmentDeletionDto.getEnvironmentDto();
        environmentService.findEnvironmentById(environmentDto.getId())
                .ifPresentOrElse(environment -> {
                            try {
                                deletePrerequisites(environmentDto, environment);
                                goToFinishedState(environmentDtoEvent);
                            } catch (Exception e) {
                                if (environmentDeletionDto.isForceDelete()) {
                                    LOGGER.warn("The %s was not successful but the environment deletion was requested " +
                                            "as force delete so continue the deletion flow", selector());
                                    goToFinishedState(environmentDtoEvent);
                                } else {
                                    goToFailedState(environmentDtoEvent, e.getMessage());
                                }
                            }
                        }, () -> goToFailedState(environmentDtoEvent, String.format("Environment was not found with id '%s'.", environmentDto.getId()))
                );
    }

    private void deletePrerequisites(EnvironmentDto environmentDto, Environment environment) {
        try {
            Optional<Setup> setupOptional = getSetupConnector(environmentDto.getCloudPlatform());
            if (setupOptional.isEmpty()) {
                LOGGER.debug("No setup defined for platform {}, resource group has not been deleted.", environmentDto.getCloudPlatform());
                return;
            }
            setupOptional.get().deleteEnvironmentPrerequisites(environmentDtoToPrerequisiteDeleteRequestConverter.convert(environmentDto));
        } catch (Exception e) {
            throw new CloudConnectorException("Could not delete resource group" + ExceptionUtils.getRootCauseMessage(e), e);
        }
    }

    private Optional<Setup> getSetupConnector(String cloudPlatform) {
        CloudPlatformVariant cloudPlatformVariant = new CloudPlatformVariant(Platform.platform(cloudPlatform), Variant.variant(cloudPlatform));
        return Optional.ofNullable(cloudPlatformConnectors.get(cloudPlatformVariant).setup());
    }

    private void goToFailedState(Event<EnvironmentDeletionDto> environmentDtoEvent, String message) {
        LOGGER.debug("Going to failed state, message: {}.", message);
        EnvironmentDeletionDto environmentDeletionDto = environmentDtoEvent.getData();
        EnvironmentDto environmentDto = environmentDeletionDto.getEnvironmentDto();
        EnvDeleteFailedEvent failureEvent = new EnvDeleteFailedEvent(
                environmentDto.getId(),
                environmentDto.getName(),
                new BadRequestException(message),
                environmentDto.getResourceCrn());

        eventSender().sendEvent(failureEvent, environmentDtoEvent.getHeaders());
    }

    private void goToFinishedState(Event<EnvironmentDeletionDto> environmentDtoEvent) {
        EnvironmentDeletionDto environmentDeletionDto = environmentDtoEvent.getData();
        EnvironmentDto environmentDto = environmentDeletionDto.getEnvironmentDto();
        EnvCreationEvent envCreationEvent = EnvCreationEvent.builder()
                .withResourceId(environmentDto.getResourceId())
                .withSelector(FINISH_ENV_DELETE_EVENT.selector())
                .withResourceCrn(environmentDto.getResourceCrn())
                .withResourceName(environmentDto.getName())
                .build();
        LOGGER.debug("Proceeding to next flow step: {}.", envCreationEvent.selector());
        eventSender().sendEvent(envCreationEvent, environmentDtoEvent.getHeaders());
    }
}
