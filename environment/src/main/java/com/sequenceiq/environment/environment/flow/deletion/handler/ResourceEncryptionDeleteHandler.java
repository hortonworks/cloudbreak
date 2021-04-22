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
import com.sequenceiq.flow.reactor.api.event.EventSender;
import com.sequenceiq.flow.reactor.api.handler.EventSenderAwareHandler;

import reactor.bus.Event;

@Component
public class ResourceEncryptionDeleteHandler extends EventSenderAwareHandler<EnvironmentDeletionDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceEncryptionDeleteHandler.class);

    private final EnvironmentService environmentService;

    private HandlerExceptionProcessor exceptionProcessor;

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
                    String diskEncryptionSetId = Optional.ofNullable(environmentDto.getParameters())
                            .map(paramsDto -> paramsDto.getAzureParametersDto())
                            .map(azureParamsDto -> azureParamsDto.getAzureResourceEncryptionParametersDto())
                            .map(azureREParamsDto -> azureREParamsDto.getDiskEncryptionSetId()).orElse(null);
                    if (StringUtils.isNotEmpty(diskEncryptionSetId) &&
                            (environment.getStatus() != EnvironmentStatus.ENVIRONMENT_ENCRYPTION_RESOURCES_DELETED)) {
                        deleteEncryptionResources(environmentDto, environment);
                    }
                }
            });
            eventSender().sendEvent(envDeleteEvent, environmentDtoEvent.getHeaders());
        } catch (Exception e) {
            exceptionProcessor.handle(new HandlerFailureConjoiner(e, environmentDtoEvent, envDeleteEvent), LOGGER, eventSender(), selector());
        }
    }

    private void deleteEncryptionResources(EnvironmentDto environmentDto, Environment environment) {
        LOGGER.info("Deleting Encryption resources for environment.");
        try {
            environmentEncryptionService.deleteEncryptionResources(environmentDto);
            environment.setStatus(EnvironmentStatus.ENVIRONMENT_ENCRYPTION_RESOURCES_DELETED);
            environmentService.save(environment);
        } catch (Exception e) {
            LOGGER.error("Failed to delete Encryption resources for environment {} with error {}", environment.getName(), e);
            throw new CloudbreakServiceException("Error occured while deleting encryption resources: " + e.getMessage(), e);
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
