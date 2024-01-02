package com.sequenceiq.environment.environment.flow.deletion.handler;

import static com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteHandlerSelectors.DELETE_PUBLICKEY_EVENT;
import static com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteStateSelectors.START_NETWORK_DELETE_EVENT;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.environment.environment.dto.EnvironmentDeletionDto;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteEvent;
import com.sequenceiq.environment.environment.service.EnvironmentResourceService;
import com.sequenceiq.environment.environment.service.EnvironmentService;
import com.sequenceiq.environment.events.EventSenderService;
import com.sequenceiq.flow.reactor.api.event.EventSender;
import com.sequenceiq.flow.reactor.api.handler.EventSenderAwareHandler;

@Component
public class PublicKeyDeleteHandler extends EventSenderAwareHandler<EnvironmentDeletionDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(PublicKeyDeleteHandler.class);

    private final HandlerExceptionProcessor exceptionProcessor;

    private final EnvironmentResourceService environmentResourceService;

    private final EnvironmentService environmentService;

    private final EventSenderService eventSenderService;

    protected PublicKeyDeleteHandler(EventSender eventSender,
            HandlerExceptionProcessor exceptionProcessor,
            EnvironmentResourceService environmentResourceService,
            EnvironmentService environmentService,
            EventSenderService eventSenderService) {
        super(eventSender);
        this.exceptionProcessor = exceptionProcessor;
        this.eventSenderService = eventSenderService;
        this.environmentResourceService = environmentResourceService;
        this.environmentService = environmentService;
    }

    @Override
    public String selector() {
        return DELETE_PUBLICKEY_EVENT.selector();
    }

    @Override
    public void accept(Event<EnvironmentDeletionDto> environmentDtoEvent) {
        LOGGER.debug("Accepting PublickeyDelete event");
        EnvironmentDeletionDto environmentDeletionDto = environmentDtoEvent.getData();
        EnvironmentDto environmentDto = environmentDeletionDto.getEnvironmentDto();
        EnvDeleteEvent envDeleteEvent = getEnvDeleteEvent(environmentDeletionDto);
        try {
            deleteManagedKey(environmentDto);
            eventSender().sendEvent(envDeleteEvent, environmentDtoEvent.getHeaders());
        } catch (Exception e) {
            exceptionProcessor.handle(new HandlerFailureConjoiner(e, environmentDtoEvent, envDeleteEvent), LOGGER, eventSender(), selector());
        }
    }

    private void deleteManagedKey(EnvironmentDto environmentDto) {
        String publicKeyId = environmentDto.getAuthentication().getPublicKeyId();
        if (environmentDto.getAuthentication().isManagedKey() && publicKeyId != null) {
            LOGGER.warn("Environment {} has managed public key. Skipping key deletion: '{}'", environmentDto.getName(), publicKeyId);
            String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
            environmentResourceService.deletePublicKey(environmentService.findEnvironmentByIdOrThrow(environmentDto.getId()));
            eventSenderService.sendEventAndNotification(environmentDto, userCrn, ResourceEvent.ENVIRONMENT_SSH_DELETION_APPLIED, List.of(publicKeyId));
        } else {
            LOGGER.debug("Environment {} had no managed public key", environmentDto.getName());
        }
    }

    private EnvDeleteEvent getEnvDeleteEvent(EnvironmentDeletionDto environmentDeletionDto) {
        EnvironmentDto environmentDto = environmentDeletionDto.getEnvironmentDto();
        return EnvDeleteEvent.builder()
                .withResourceId(environmentDto.getResourceId())
                .withResourceName(environmentDto.getName())
                .withResourceCrn(environmentDto.getResourceCrn())
                .withForceDelete(environmentDeletionDto.isForceDelete())
                .withSelector(START_NETWORK_DELETE_EVENT.selector())
                .build();
    }

}
