package com.sequenceiq.environment.environment.flow.encryptionprofile.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.environment.environment.flow.encryptionprofile.event.DisableEncryptionProfileEvent;
import com.sequenceiq.environment.environment.flow.encryptionprofile.event.DisableEncryptionProfileFailedEvent;
import com.sequenceiq.environment.environment.flow.encryptionprofile.event.DisableEncryptionProfileStateSelectors;
import com.sequenceiq.environment.environment.service.EnvironmentService;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class UnsetEncryptionProfileHandler extends ExceptionCatcherEventHandler<DisableEncryptionProfileEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(UnsetEncryptionProfileHandler.class);

    private final EnvironmentService environmentService;

    protected UnsetEncryptionProfileHandler(EnvironmentService environmentService) {
        this.environmentService = environmentService;
    }

    @Override
    public String selector() {
        return DisableEncryptionProfileStateSelectors.UNSET_ENCRYPTION_PROFILE_HANDLER_EVENT.name();
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<DisableEncryptionProfileEvent> event) {
        return DisableEncryptionProfileFailedEvent.from(event.getData(), e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<DisableEncryptionProfileEvent> event) {
        DisableEncryptionProfileEvent payload = event.getData();
        LOGGER.info("Removing encryption profile CRN from environment {}", payload.getResourceName());
        environmentService.disableEncryptionProfile(payload.getResourceCrn());
        LOGGER.info("Encryption profile CRN removed from environment {}", payload.getResourceName());
        return DisableEncryptionProfileEvent
                .builder()
                .withSelector(DisableEncryptionProfileStateSelectors.DISABLE_ENCRYPTION_PROFILE_ON_FREEIPA_EVENT.selector())
                .withResourceId(payload.getResourceId())
                .withResourceName(payload.getResourceName())
                .withResourceCrn(payload.getResourceCrn())
                .build();
    }
}
