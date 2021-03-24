package com.sequenceiq.environment.environment.flow.creation.handler;

import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.AZURE;
import static com.sequenceiq.environment.environment.flow.creation.event.EnvCreationHandlerSelectors.INITIALIZE_ENVIRONMENT_RESOURCE_ENCRYPTION_EVENT;
import static com.sequenceiq.environment.environment.flow.creation.event.EnvCreationStateSelectors.START_FREEIPA_CREATION_EVENT;

import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.flow.creation.event.EnvCreationEvent;
import com.sequenceiq.environment.environment.flow.creation.event.EnvCreationFailureEvent;
import com.sequenceiq.environment.environment.service.EnvironmentService;
import com.sequenceiq.flow.reactor.api.event.EventSender;
import com.sequenceiq.flow.reactor.api.handler.EventSenderAwareHandler;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class ResourceEncryptionInitializationHandler extends EventSenderAwareHandler<EnvironmentDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceEncryptionInitializationHandler.class);

    private final EnvironmentService environmentService;

    private final EntitlementService entitlementService;

    private final EventBus eventBus;

    protected ResourceEncryptionInitializationHandler(EventSender eventSender, EnvironmentService environmentService, EventBus eventBus,
            EntitlementService entitlementService) {
        super(eventSender);
        this.environmentService = environmentService;
        this.eventBus = eventBus;
        this.entitlementService = entitlementService;
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
                            .map(paramsDto -> paramsDto.getAzureParametersDto())
                            .map(azureParamsDto -> azureParamsDto.getAzureResourceEncryptionParametersDto())
                            .map(azureREParamsDto -> azureREParamsDto.getEncryptionKeyUrl()).orElse(null);
                    if (StringUtils.isNotEmpty(encryptionKeyUrl)) {
                        LOGGER.debug("Creating DiskEncryptionSet for environment.");
                            /*
                            TO DO - create the Disk Encryption Set and save environment.
                            */
                    } else {
                        LOGGER.debug("Environment {} has not requested for SSE with CMK.", environment.getName());
                    }
                }
            });
            EnvCreationEvent envCreationEvent = getEnvCreateEvent(environmentDto);
            eventSender().sendEvent(envCreationEvent, environmentDtoEvent.getHeaders());
        } catch (Exception e) {
            LOGGER.debug("ResourceEncryptionInitialization failed with error.", e);
            EnvCreationFailureEvent failedEvent =
                    new EnvCreationFailureEvent(environmentDto.getId(), environmentDto.getName(), e, environmentDto.getResourceCrn());
            Event<EnvCreationFailureEvent> ev = new Event<>(environmentDtoEvent.getHeaders(), failedEvent);
            eventBus.notify(failedEvent.selector(), ev);
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

