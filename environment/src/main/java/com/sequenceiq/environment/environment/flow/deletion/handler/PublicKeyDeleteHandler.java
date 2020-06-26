package com.sequenceiq.environment.environment.flow.deletion.handler;

import static com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteHandlerSelectors.DELETE_PUBLICKEY_EVENT;
import static com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteStateSelectors.START_NETWORK_DELETE_EVENT;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteEvent;
import com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteFailedEvent;
import com.sequenceiq.environment.environment.service.EnvironmentResourceService;
import com.sequenceiq.environment.environment.service.EnvironmentService;
import com.sequenceiq.flow.reactor.api.event.EventSender;
import com.sequenceiq.flow.reactor.api.handler.EventSenderAwareHandler;

import reactor.bus.Event;

@Component
public class PublicKeyDeleteHandler extends EventSenderAwareHandler<EnvironmentDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(PublicKeyDeleteHandler.class);

    private final EnvironmentService environmentService;

    private final EnvironmentResourceService environmentResourceService;

    protected PublicKeyDeleteHandler(EventSender eventSender, EnvironmentService environmentService, EnvironmentResourceService environmentResourceService) {

        super(eventSender);
        this.environmentService = environmentService;
        this.environmentResourceService = environmentResourceService;
    }

    @Override
    public String selector() {
        return DELETE_PUBLICKEY_EVENT.selector();
    }

    @Override
    public void accept(Event<EnvironmentDto> environmentDtoEvent) {
        LOGGER.debug("Accepting PublickeyDelete event");
        EnvironmentDto environmentDto = environmentDtoEvent.getData();
        try {
            environmentService.findEnvironmentById(environmentDto.getId()).ifPresent(this::deleteManagedKey);
            EnvDeleteEvent envDeleteEvent = getEnvDeleteEvent(environmentDto);
            eventSender().sendEvent(envDeleteEvent, environmentDtoEvent.getHeaders());
        } catch (Exception e) {
            EnvDeleteFailedEvent failedEvent = new EnvDeleteFailedEvent(environmentDto.getId(), environmentDto.getName(), e, environmentDto.getResourceCrn());
            eventSender().sendEvent(failedEvent, environmentDtoEvent.getHeaders());
        }
    }

    private void deleteManagedKey(Environment environment) {
        if (environment.getAuthentication().isManagedKey() && environment.getAuthentication().getPublicKeyId() != null) {
            LOGGER.debug("Environment {} has managed public key. Deleting.", environment.getName());
            environmentResourceService.deletePublicKey(environment);
        } else {
            LOGGER.debug("Environment {} had no managed public key", environment.getName());
        }
    }

    private EnvDeleteEvent getEnvDeleteEvent(EnvironmentDto environmentDto) {
        return EnvDeleteEvent.builder()
                .withResourceId(environmentDto.getResourceId())
                .withResourceName(environmentDto.getName())
                .withResourceCrn(environmentDto.getResourceCrn())
                .withSelector(START_NETWORK_DELETE_EVENT.selector())
                .build();
    }
}
