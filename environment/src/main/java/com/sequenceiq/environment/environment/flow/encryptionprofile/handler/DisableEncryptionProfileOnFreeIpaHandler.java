package com.sequenceiq.environment.environment.flow.encryptionprofile.handler;

import static com.sequenceiq.environment.environment.flow.encryptionprofile.event.DisableEncryptionProfileStateSelectors.DISABLE_ENCRYPTION_PROFILE_ON_FREEIPA_HANDLER_EVENT;
import static com.sequenceiq.environment.environment.flow.encryptionprofile.event.DisableEncryptionProfileStateSelectors.DISABLE_ENCRYPTION_PROFILE_ON_STACKS_EVENT;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.environment.environment.flow.encryptionprofile.event.DisableEncryptionProfileEvent;
import com.sequenceiq.environment.environment.flow.encryptionprofile.event.DisableEncryptionProfileFailedEvent;
import com.sequenceiq.environment.environment.service.freeipa.FreeIpaPollerService;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class DisableEncryptionProfileOnFreeIpaHandler extends ExceptionCatcherEventHandler<DisableEncryptionProfileEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DisableEncryptionProfileOnFreeIpaHandler.class);

    private final FreeIpaPollerService freeIpaPollerService;

    protected DisableEncryptionProfileOnFreeIpaHandler(FreeIpaPollerService freeIpaPollerService) {
        this.freeIpaPollerService = freeIpaPollerService;
    }

    @Override
    public String selector() {
        return DISABLE_ENCRYPTION_PROFILE_ON_FREEIPA_HANDLER_EVENT.name();
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<DisableEncryptionProfileEvent> event) {
        return DisableEncryptionProfileFailedEvent.from(event.getData(), e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<DisableEncryptionProfileEvent> event) {
        DisableEncryptionProfileEvent payload = event.getData();
        LOGGER.info("Updating salt on FreeIPA to restore the default encryption profile for environment {}", payload.getResourceName());
        freeIpaPollerService.waitForSaltUpdate(payload.getResourceId(), payload.getResourceCrn());
        LOGGER.info("Salt updated on FreeIPA for environment {}", payload.getResourceName());
        return DisableEncryptionProfileEvent.builder()
                .withSelector(DISABLE_ENCRYPTION_PROFILE_ON_STACKS_EVENT.selector())
                .withResourceId(payload.getResourceId())
                .withResourceName(payload.getResourceName())
                .withResourceCrn(payload.getResourceCrn())
                .build();
    }
}
