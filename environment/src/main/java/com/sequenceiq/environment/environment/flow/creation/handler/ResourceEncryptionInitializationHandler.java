package com.sequenceiq.environment.environment.flow.creation.handler;

import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.AZURE;
import static com.sequenceiq.environment.environment.flow.creation.event.EnvCreationHandlerSelectors.INITIALIZE_ENVIRONMENT_RESOURCE_ENCRYPTION_EVENT;
import static com.sequenceiq.environment.environment.flow.creation.event.EnvCreationStateSelectors.START_FREEIPA_CREATION_EVENT;

import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.encryption.CreatedDiskEncryptionSet;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.encryption.EnvironmentEncryptionService;
import com.sequenceiq.environment.environment.flow.creation.event.EnvCreationEvent;
import com.sequenceiq.environment.environment.flow.creation.event.EnvCreationFailureEvent;
import com.sequenceiq.environment.environment.service.EnvironmentService;
import com.sequenceiq.environment.parameter.dto.AzureParametersDto;
import com.sequenceiq.environment.parameter.dto.AzureResourceEncryptionParametersDto;
import com.sequenceiq.environment.parameter.dto.ParametersDto;
import com.sequenceiq.environment.parameters.dao.domain.AzureParameters;
import com.sequenceiq.flow.reactor.api.event.EventSender;
import com.sequenceiq.flow.reactor.api.handler.EventSenderAwareHandler;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class ResourceEncryptionInitializationHandler extends EventSenderAwareHandler<EnvironmentDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceEncryptionInitializationHandler.class);

    private final EnvironmentService environmentService;

    private final EnvironmentEncryptionService environmentEncryptionService;

    private final EventBus eventBus;

    protected ResourceEncryptionInitializationHandler(EventSender eventSender, EnvironmentService environmentService, EventBus eventBus,
            EnvironmentEncryptionService environmentEncryptionService) {
        super(eventSender);
        this.environmentService = environmentService;
        this.eventBus = eventBus;
        this.environmentEncryptionService = environmentEncryptionService;
    }

    @Override
    public String selector() {
        return INITIALIZE_ENVIRONMENT_RESOURCE_ENCRYPTION_EVENT.selector();
    }

    @Override
    public void accept(Event<EnvironmentDto> environmentDtoEvent) {
        LOGGER.debug("Accepting ResourceEncryptionInitialization event");
        EnvironmentDto environmentDto = environmentDtoEvent.getData();
        try {
            environmentService.findEnvironmentById(environmentDto.getId()).ifPresent(environment -> {
                if (AZURE.name().equalsIgnoreCase(environmentDto.getCloudPlatform())) {
                    String encryptionKeyUrl = Optional.ofNullable(environmentDto.getParameters())
                            .map(ParametersDto::getAzureParametersDto)
                            .map(AzureParametersDto::getAzureResourceEncryptionParametersDto)
                            .map(AzureResourceEncryptionParametersDto::getEncryptionKeyUrl).orElse(null);
                    String environmentName = environment.getName();
                    if (StringUtils.isNotEmpty(encryptionKeyUrl)) {
                        if (environment.getStatus() != EnvironmentStatus.ENVIRONMENT_ENCRYPTION_RESOURCES_INITIALIZED) {
                            initializeEncryptionResources(environmentDto, environment);
                        } else {
                            LOGGER.info("Initialization of encryption resources for environment \"{}\" has already been triggered, " +
                                    "continuing without new initialize trigger. Environment status: {}", environmentName, environment.getStatus());
                        }
                    } else {
                        LOGGER.info("Environment \"{}\" has not requested for SSE with CMK.", environmentName);
                    }
                }
            });
            EnvCreationEvent envCreationEvent = getEnvCreateEvent(environmentDto);
            eventSender().sendEvent(envCreationEvent, environmentDtoEvent.getHeaders());
        } catch (Exception e) {
            LOGGER.error("ResourceEncryptionInitialization failed with error.", e);
            EnvCreationFailureEvent failedEvent =
                    new EnvCreationFailureEvent(environmentDto.getId(), environmentDto.getName(), e, environmentDto.getResourceCrn());
            Event<EnvCreationFailureEvent> ev = new Event<>(environmentDtoEvent.getHeaders(), failedEvent);
            eventBus.notify(failedEvent.selector(), ev);
        }
    }

    private void initializeEncryptionResources(EnvironmentDto environmentDto, Environment environment) {
        String environmentName = environment.getName();
        LOGGER.info("Initializing encryption resources for environment \"{}\".", environmentName);
        try {
            CreatedDiskEncryptionSet createdDiskEncryptionSet = environmentEncryptionService.createEncryptionResources(environmentDto);
            LOGGER.info("Created Disk Encryption Set resource for environment \"{}\": {}", environmentName, createdDiskEncryptionSet);
            AzureParameters azureParameters = (AzureParameters) environment.getParameters();
            azureParameters.setDiskEncryptionSetId(createdDiskEncryptionSet.getDiskEncryptionSetId());
            environment.setStatus(EnvironmentStatus.ENVIRONMENT_ENCRYPTION_RESOURCES_INITIALIZED);
            environment.setStatusReason(null);
            environmentService.save(environment);
            LOGGER.info("Finished initializing encryption resources for environment \"{}\".", environmentName);
        } catch (Exception e) {
            LOGGER.error(String.format("Failed to initialize encryption resources for environment \"%s\"", environmentName), e);
            throw new CloudbreakServiceException("Error occurred while initializing encryption resources: " + e.getMessage(), e);
        }
    }

    private EnvCreationEvent getEnvCreateEvent(EnvironmentDto environmentDto) {
        return EnvCreationEvent.builder()
                .withResourceId(environmentDto.getResourceId())
                .withResourceName(environmentDto.getName())
                .withResourceCrn(environmentDto.getResourceCrn())
                .withSelector(START_FREEIPA_CREATION_EVENT.selector())
                .build();
    }

}

