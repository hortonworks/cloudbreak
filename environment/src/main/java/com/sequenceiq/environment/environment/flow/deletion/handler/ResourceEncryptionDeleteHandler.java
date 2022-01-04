package com.sequenceiq.environment.environment.flow.deletion.handler;

import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.AZURE;
import static com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteHandlerSelectors.DELETE_ENVIRONMENT_RESOURCE_ENCRYPTION_EVENT;
import static com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteStateSelectors.START_PUBLICKEY_DELETE_EVENT;

import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.dto.EnvironmentDeletionDto;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.encryption.EnvironmentEncryptionService;
import com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteEvent;
import com.sequenceiq.environment.environment.service.EnvironmentService;
import com.sequenceiq.environment.parameter.dto.AzureParametersDto;
import com.sequenceiq.environment.parameter.dto.AzureResourceEncryptionParametersDto;
import com.sequenceiq.environment.parameter.dto.ParametersDto;
import com.sequenceiq.flow.reactor.api.event.EventSender;
import com.sequenceiq.flow.reactor.api.handler.EventSenderAwareHandler;

import reactor.bus.Event;

@Component
public class ResourceEncryptionDeleteHandler extends EventSenderAwareHandler<EnvironmentDeletionDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceEncryptionDeleteHandler.class);

    private final EnvironmentService environmentService;

    private final HandlerExceptionProcessor exceptionProcessor;

    private final EnvironmentEncryptionService environmentEncryptionService;

    protected ResourceEncryptionDeleteHandler(EventSender eventSender, EnvironmentService environmentService,
            EnvironmentEncryptionService environmentEncryptionService, HandlerExceptionProcessor exceptionProcessor) {
        super(eventSender);
        this.environmentService = environmentService;
        this.exceptionProcessor = exceptionProcessor;
        this.environmentEncryptionService = environmentEncryptionService;
    }

    @Override
    public String selector() {
        return DELETE_ENVIRONMENT_RESOURCE_ENCRYPTION_EVENT.selector();
    }

    @Override
    public void accept(Event<EnvironmentDeletionDto> environmentDtoEvent) {
        LOGGER.debug("Accepting ResourceEncryptionDelete event");
        EnvironmentDeletionDto environmentDeletionDto = environmentDtoEvent.getData();
        EnvironmentDto environmentDto = environmentDeletionDto.getEnvironmentDto();
        EnvDeleteEvent envDeleteEvent = getEnvDeleteEvent(environmentDeletionDto);
        try {
            environmentService.findEnvironmentById(environmentDto.getId()).ifPresent(environment -> {
                if (AZURE.name().equalsIgnoreCase(environmentDto.getCloudPlatform())) {
                    String encryptionKeyUrl = Optional.ofNullable(environmentDto.getParameters())
                            .map(ParametersDto::getAzureParametersDto)
                            .map(AzureParametersDto::getAzureResourceEncryptionParametersDto)
                            .map(AzureResourceEncryptionParametersDto::getEncryptionKeyUrl)
                            .orElse(null);
                    if (StringUtils.isNotEmpty(encryptionKeyUrl) &&
                            (environment.getStatus() != EnvironmentStatus.ENVIRONMENT_ENCRYPTION_RESOURCES_DELETED)) {
                        deleteEncryptionResources(environmentDto, environment);
                    } else {
                        LOGGER.info("No encryption resources found to delete for environment \"{}\".", environment.getName());
                    }
                }
            });
            eventSender().sendEvent(envDeleteEvent, environmentDtoEvent.getHeaders());
        } catch (Exception e) {
            LOGGER.error("ResourceEncryptionDelete failed with error.", e);
            exceptionProcessor.handle(new HandlerFailureConjoiner(e, environmentDtoEvent, envDeleteEvent), LOGGER, eventSender(), selector());
        }
    }

    private void deleteEncryptionResources(EnvironmentDto environmentDto, Environment environment) {
        String environmentName = environment.getName();
        LOGGER.info("Deleting encryption resources for environment \"{}\".", environmentName);
        try {
            environmentEncryptionService.deleteEncryptionResources(environmentDto);
            environment.setStatus(EnvironmentStatus.ENVIRONMENT_ENCRYPTION_RESOURCES_DELETED);
            environment.setStatusReason(null);
            environmentService.save(environment);
            LOGGER.info("Finished deleting encryption resources for environment \"{}\".", environmentName);
        } catch (Exception e) {
            LOGGER.error(String.format("Failed to delete encryption resources for environment \"%s\"", environmentName), e);
            throw new CloudbreakServiceException("Error occurred while deleting encryption resources: " + e.getMessage(), e);
        }
    }

    private EnvDeleteEvent getEnvDeleteEvent(EnvironmentDeletionDto environmentDeletionDto) {
        EnvironmentDto environmentDto = environmentDeletionDto.getEnvironmentDto();
        return EnvDeleteEvent.builder()
                .withResourceId(environmentDto.getResourceId())
                .withResourceName(environmentDto.getName())
                .withResourceCrn(environmentDto.getResourceCrn())
                .withForceDelete(environmentDeletionDto.isForceDelete())
                .withSelector(START_PUBLICKEY_DELETE_EVENT.selector())
                .build();
    }

}
